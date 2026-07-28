# Reproducible Configuration Assembly

## Motivation

An RX application currently discovers and merges configuration from indexed
classpath resources, the packaged filesystem mirror and the deployment
`conf/` directory. This preserves composability, but a terminal application
build does not yet prove that its packaged configuration is complete.
Undeclared placeholders, incompatible fragments or missing sibling
configuration can consequently remain undetected until the application starts
or a particular configuration is first used.

The terminal application build should therefore produce and validate an
explicit static configuration closure. This closure is not a replacement for
the source resources. It is a reproducible, inspectable view derived from them.

The design has four goals:

1. Validate configuration artifacts as early as possible.
2. Prove at application build time that every non-imported property can be
   resolved.
3. Preserve legitimate deployment imports symbolically.
4. Retain source provenance and backward-compatible runtime layering.

## Configuration views

An assembled application exposes three distinct views:

```text
application/
  classpath-resources/     # immutable source/provenance mirror
  packaged-conf/
    effective/             # canonical static closure
    residual/              # configuration assets without an assembler
    assembly-report.yaml   # origins, imports, coverage and diagnostics
  conf/                    # deployment overlays
```

`classpath-resources/` remains suitable for diagnostics and contains the
unmodified indexed resources grouped by artifact. Successfully assembled
source entries must, however, be shadowed in the runtime index so that the raw
fragments and their effective result are not merged twice.

`packaged-conf/effective/` contains one canonical result per configuration key
where the corresponding assembler supports such a representation. A
configuration key consists at least of configuration kind, modeled type and
use case.

Assets which cannot yet be assembled stay available below `residual/` with
their artifact association. They are reported explicitly and retain their
existing runtime behavior.

## Explicit deployment imports

Packaged properties are closed by default. A property placeholder may remain
unresolved only when it is declared as a deployment import.

Imports are declared in the separately modeled
`configuration-imports.yaml`:

```yaml
imports:
  - name: DB_DEFAULT_HOST
    required: true
    confidential: false
    description: PostgreSQL host supplied by the deployment

  - name: DB_DEFAULT_PASSWORD
    required: true
    confidential: true
    description: PostgreSQL password supplied by the deployment
```

Import names are exact rather than pattern based. Import declarations from
multiple configuration artifacts are merged. Incompatible declarations for
the same name fail assembly.

The declaration is artifact metadata, not runtime configuration. A
configuration artifact packages it at the stable location
`META-INF/configuration-imports.yaml`; a terminal application may contribute
the same metadata. This is deliberately outside `HICONIC-CONF`, so the modeled
configuration loader never mistakes the declaration for another configuration
layer. The declaration must nevertheless travel with the artifact: keeping it
only in a source repository or build configuration would make transitive
terminal assembly incomplete.

The small declaration model belongs to a central GM
`configuration-assembly-model` artifact. It must not belong to an Ant task:
the application-classpath assembler, artifact validators, runtime diagnostics
and future tooling all need the same semantics without depending on the build
system. The terminal assembler aggregates the artifact declarations into its
assembly report. During the backward-compatible rollout, runtime and build-time
assembly use the same declaration reader over the indexed artifact resources.
Once the effective view is authoritative, runtime may consume its aggregated
report without changing import semantics.

Semantics:

- Internal properties must be fully resolvable from packaged properties and
  platform-defined inputs.
- Imports are symbolic leaves and are never accidentally resolved from the
  build machine or CI environment.
- Derived properties may be partially evaluated while retaining imported
  variables.
- Undeclared unresolved variables and property cycles are assembly errors.
- `required` imports are validated early during runtime startup.
- `confidential` imports are redacted in diagnostics and reports.
- Defaults remain property definitions in `properties.yaml`; they are not
  duplicated in the import declaration.

### Managed runtime binding

An environment variable is not itself a configuration layer. In the managed
regime, deployment values enter configuration through one explicit binding
step:

```text
declared import
  -> system property / environment lookup
  -> managed raw-property map
  -> normal property and modeled-configuration resolution
```

Only exact declared import names are looked up. A packaged or filesystem
property of the same name takes precedence and can therefore satisfy a
required import for local or test setups without consulting the host.
System properties take precedence over environment variables when both supply
the same declared import.

Missing `required` imports fail application startup before module wiring.
Missing optional imports remain absent. After binding, the runtime property
resolver has no implicit system-property, environment-variable or
`env.`-prefixed fallback: processors and configuration consumers observe only
the managed property graph.

For migration safety this strict mode is activated by the presence of at least
one `META-INF/configuration-imports.yaml`. Existing applications without an
import descriptor retain the legacy fallback behavior. An intentionally empty
descriptor is therefore also a deliberate opt-in to managed properties.
Direct users of `RxPropertyResolver` retain legacy behavior unless they
explicitly enable its managed-only mode. The older
`EnvironmentPropertiesContract` is not changed implicitly by this rollout; its
eventual migration is a separately auditable step.

Platform-provided variables such as `reflex.app.dir` form a separate standard
input set. They remain symbolic but are reported as platform variables rather
than deployment imports; an operator must not be told to supply them.
Source-location variables such as `config.file` and `config.dir` require
special handling because moving a configuration into the effective view must
not silently change their meaning.

## Symbolic placeholder preservation

Modeled YAML already provides the required foundation:

- placeholder-aware unmarshalling can retain `Variable` and `Concatenation`
  value descriptors;
- unresolved imported variables can therefore survive merging;
- the YAML marshaller can write these descriptors back as ordinary
  `${...}` expressions when `PlaceholderSupport` is enabled.

The assembler uses a partial symbolic property evaluator rather than the
strict runtime resolver. For example:

```yaml
DB_DEFAULT_PORT: "5432"
DB_DEFAULT_NAME: "proventem"
DB_DEFAULT_URL: "jdbc:postgresql://${DB_DEFAULT_HOST}:${DB_DEFAULT_PORT}/${DB_DEFAULT_NAME}"
```

is validated against the external leaf `DB_DEFAULT_HOST`, although the first
writer deliberately preserves the named property boundary:

```yaml
endpoint: "${DB_DEFAULT_URL}"
```

This retains the author’s named configuration abstraction while proving that
its only external dependency is the declared import. Partial inlining to an
equivalent expression such as
`jdbc:postgresql://${DB_DEFAULT_HOST}:5432/proventem` is a possible later
normalization, not a prerequisite for closure validation.

The written result is parsed again and compared semantically with the
assembled entity. It is then serialized again and must produce byte-identical
canonical YAML. This round trip is part of validation.

## Configuration assembler SPI

Different configuration families have different composition semantics. They
are handled through a small SPI rather than one generic file merger:

```text
ConfigurationAssetAssembler
  discover
  classify
  assemble
  validate
  write
```

Initial assemblers:

1. layered properties;
2. modeled YAML configuration;
3. log-level configuration;
4. Logback configuration.

An assembler may produce one canonical file or a canonical ordered plan with
fragments. Logback configuration, for example, should not be flattened when
its natural semantics are an ordered application of fragments.

Unknown assets are retained and reported. Strictness can later be raised once
all intended configuration families have assemblers.

## Execution on the application classpath

Configuration assembly is executed by a dedicated main class on the real
application classpath:

```text
java -cp <application-classpath> \
  hiconic.rx.platform.configuration.ConfigurationAssemblyMain \
  --application-dir <application-dir>
```

The main class initializes application model reflection, the indexed resource
view and the assembler registry. It does not start the web server or normal
application lifecycle.

The reusable implementation is carried by the regular
`configuration-assembly-processing` dependency. Configuration aggregators
propagate it naturally through setups to application terminals. This is
intentional:

- Maven `test` would misclassify configuration assembly as testing;
- Maven `provided` is flat and would have to be repeated on every aggregator
  and terminal;
- neither scope expresses the required union of the application's runtime
  graph and build-time processing;
- the marginal runtime footprint is small because model reflection, modeled
  configuration and YAML processing are already present;
- the same processing is useful for runtime diagnostics and future
  configuration reflection.

The launcher remains a narrow `main(String[])` adapter with filesystem
arguments. It introduces no modeled command API and contains no artifact
resolution. This avoids mixing the build-tool classpath with the application
classpath: the RX application Ant script only launches a forked Java process
after dependencies and indexed resources have been materialized.

The core implementation and launcher now exist and are tested independently
of the application Ant script. Activating the launcher in application assembly
is a separate rollout step: the runtime shadow/index representation must first
ensure that an effective entry replaces, rather than supplements, its raw
fragments.

Configuration type discovery initially maps the canonical kebab-case filename
to application entity short names:

- no matching type leaves an explicitly reported residual;
- multiple matching types are an assembly error;
- the inferred type is used for typed unmarshalling and merging.

A later configuration-type index or model marker can make discovery smaller
and completely explicit without being a prerequisite for the first version.

## Static and runtime-effective configuration

The first implementation deliberately closes static configuration only:

```text
classpath fragments + packaged properties -> effective static configuration
```

Code-registered configuration contributions continue to be applied by the
normal RX runtime at their existing stages. The assembly report states this
coverage boundary and must not claim that the static result is the complete
runtime state.

A later phase may bootstrap the application only through configuration
registration and include code contributions. That extension needs separate
analysis because module wiring may create side effects and dependencies which
do not belong in a build-time validation process.

## Deployment replacement

Normal files in `conf/` remain overlays. An emergency deployment can explicitly
replace the packaged static configuration for one modeled type and use case
with the reserved disambiguator:

```text
external-tools-configuration.replace-packaged.yaml
```

This suppresses the packaged static contribution for that key and begins the
filesystem layer with the replacement file. It does not implicitly suppress
later code-registered contributions.

## Artifact-level validation

The same assemblers can validate individual configuration artifacts before
terminal application assembly:

- indexed resources are syntactically valid;
- canonical filenames map unambiguously to configuration types;
- fragments can be unmarshalled with absent-property semantics;
- locally defined property graphs contain no cycles;
- import declarations are well formed;
- placeholder serialization round trips correctly.

Artifact validation cannot prove terminal completeness because dependencies
and deployment imports are not yet closed. It nevertheless moves structural
errors to the earliest meaningful build.

## Backward-compatible rollout

The feature is additive:

- applications without an assembly report retain the existing loader;
- raw classpath resources remain available for diagnostics;
- residual configuration retains its current runtime behavior;
- effective entries shadow only the raw entries from which they were built;
- `conf/` overlays continue to work;
- CX is unaffected;
- configuration artifacts do not need to change before the first application
  adopts assembly.

This permits incremental rollout per application while the assemblers and
coverage become progressively stricter.

## Implementation status

The first hardened increment provides:

- merged and conflict-checked import declarations;
- closed local property evaluation and tracing of symbolic aliases to their
  true external leaves;
- discovery and typed merge of modeled configuration across classpath and
  filesystem layers;
- explicit use-case identity and ambiguity detection;
- undeclared-import and property-cycle failures;
- explicit separation of deployment imports and platform-supplied variables;
- canonical effective YAML and assembly-report output;
- placeholder-preserving parse/serialize stability checks;
- a thin application-classpath command-line launcher.

Residual materialization with full artifact provenance, runtime shadow-index
activation, further configuration-family assemblers and Ant integration remain
deliberately outside this increment.

## Proposed implementation sequence

1. Model `ConfigurationImports` and the assembly report.
2. Implement the symbolic property graph and round-trip tests.
3. Extract modeled YAML discovery, sorting and merging into a reusable
   assembler.
4. Test artifact-level validation with synthetic configuration artifacts.
5. Test terminal closure with multiple artifacts, use cases and imports.
6. Add residual handling and runtime shadowing.
7. Invoke the assembly main from the RX application Ant script behind an
   explicit compatibility switch.
8. Enable it for one RX application and compare runtime semantics before
   making it the default.
