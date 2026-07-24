# Hiconic entities in TypeScript

This web application exchanges modeled entities with Reflex through `hiconic.js`.
The generated declarations in each `@dev.hiconic/*/dist/*.d.ts` package are the
source of truth. Do not translate remembered Java API calls into TypeScript.

The richer reference implementation is the sibling `hiconic-explorer`, especially
`terminals/explorer-terminal/src/values.ts`, `fields.ts`, and `api.ts`.

## Entity types and instances

A generated model export is both the TypeScript entity namespace member and its
runtime `EntityType`:

```ts
import { LogFilter, LogPropertyFilter } from "@dev.hiconic/platform.reflex_log-reflection-api-model";

const filter = LogFilter.create();
const propertyFilters = new T.Array(LogPropertyFilter);
```

Do not use Java-style constructs such as `LogFilter.getType()`. For an existing
entity, runtime reflection starts at `entity.EntityType()`:

```ts
const type = entity.EntityType();
const value = type.getProperty("message").get(entity);
```

Generic code should enumerate `entity.PropertyNames()` and read values through
`entity.EntityType().getProperty(name).get(entity)`. `Object.keys()` and
`Object.entries()` expose implementation details rather than modeled properties.

## Modeled collections

Request properties declared as lists, sets, or maps must receive typed Hiconic
collections, including when empty:

```ts
const levels = new T.Set(LogLevel[hc.Symbol.enumType]);
const names = new T.Set(hc.reflection.STRING);
const filters = new T.Array(LogPropertyFilter);
const values = new T.Map(hc.reflection.STRING, hc.reflection.STRING);
```

Use the model's element `EntityType`, the enum's `[hc.Symbol.enumType]`, or a
primitive reflection type. Never infer a collection type via a Java-style
property-reflection chain.

Hiconic collections provide their own collection API. Prefer `values()`,
`entries()`, or their iterator. Do not use `Object.entries()` on a modeled map.
When TypeScript cannot infer ECMAScript iterability, bridge explicitly while
retaining the modeled element type:

```ts
Array.from(list as unknown as Iterable<LogSegmentDescriptor>);
```

## Enums

Use the generated enum constants as values and the enum reflection type for
collections:

```ts
levels.add(LogLevel.ERROR);
new T.Set(LogLevel[hc.Symbol.enumType]);
```

For display, an enum value exposes `name()`; it is not necessarily a plain
JavaScript string or object with a string-valued `name` property.

## Service evaluation

Requests are created from their generated `EntityType` and evaluated through the
domain evaluator:

```ts
const request = QueryLogRecords.create();
const maybe = await request.EvalAndGetReasoned(evaluator);

if (maybe.isUnsatisfied())
  throw new Error(maybe.whyUnsatisfied().Stringify());

const page = maybe.get();
```

Keep `Maybe`/reason handling at the transport boundary. UI code should receive
the modeled success value or a normal `Error`.

## Review checklist

- Every modeled request/response is treated as a Hiconic entity, not a plain DTO.
- Every assigned collection is a correctly typed `T.Array`, `T.Set`, or `T.Map`.
- Reflection starts from the generated `EntityType` or `entity.EntityType()`.
- Modeled maps and entities are not traversed with `Object.entries()`.
- Enum values and enum reflection types are not confused.
- `EvalAndGetReasoned` failures preserve their modeled reason text.

