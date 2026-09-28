// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.platform.loading;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.gm.model.reason.essential.NotFound;

import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.RxArtifactDescriptorReader.OptionalModuleDescriptor;
import hiconic.rx.platform.loading.RxArtifactDescriptorReader.RxArtifactDescriptor;

/** Discovers RX modules without loading descriptor classes and resolves the active module set. */
/* package */ class WireModuleLoader {

	private static final String LEGACY_DESCRIPTOR = "META-INF/rx-module.properties";
	private static final String ARTIFACT_DESCRIPTOR = "rx/package-info.class";

	public static Maybe<LoadedRxModules> loadWireModules() {
		try {
			return resolve(readCatalog());
		} catch (IOException e) {
			return Reasons.build(IoError.T).text("Could not read RX module descriptors").cause(InternalError.from(e)).toMaybe();
		}
	}

	private static Catalog readCatalog() throws IOException {
		Catalog catalog = new Catalog();
		ClassLoader classLoader = WireModuleLoader.class.getClassLoader();

		Enumeration<URL> descriptors = classLoader.getResources(ARTIFACT_DESCRIPTOR);
		while (descriptors.hasMoreElements())
			catalog.add(RxArtifactDescriptorReader.read(descriptors.nextElement()));

		Enumeration<URL> legacyDescriptors = classLoader.getResources(LEGACY_DESCRIPTOR);
		while (legacyDescriptors.hasMoreElements())
			catalog.addLegacy(readLegacyDescriptor(legacyDescriptors.nextElement()));

		return catalog;
	}

	private static List<String> readLegacyDescriptor(URL url) throws IOException {
		Properties properties = new Properties();
		try (Reader reader = new InputStreamReader(url.openStream(), "UTF-8")) {
			properties.load(reader);
		}

		String value = properties.getProperty("wire-module");
		if (value == null)
			throw new IOException("Missing property 'wire-module' in " + url);

		List<String> modules = new ArrayList<>();
		for (String module : value.split(",")) {
			String trimmed = module.trim();
			if (!trimmed.isEmpty())
				modules.add(trimmed);
		}
		if (modules.isEmpty())
			throw new IOException("Property 'wire-module' is empty in " + url);
		return modules;
	}

	private static Maybe<LoadedRxModules> resolve(Catalog catalog) {
		try {
			return Maybe.complete(new Resolver(catalog).resolve());
		} catch (ModuleConfigurationException e) {
			return ConfigurationError.create(e.getMessage()).asMaybe();
		}
	}

	private static Maybe<RxModule<?>> loadRxModule(String moduleName) {
		Class<?> moduleClass;
		try {
			moduleClass = Class.forName(moduleName);
		} catch (ClassNotFoundException e) {
			return NotFound.create("Class not found: " + moduleName).asMaybe();
		}

		if (!moduleClass.isEnum())
			return InvalidArgument.create("Class is not an enum: " + moduleName).asMaybe();

		@SuppressWarnings("rawtypes")
		Class<? extends Enum> enumClass = (Class<? extends Enum>) moduleClass;
		Enum<?> constant;
		try {
			constant = Enum.valueOf(enumClass, "INSTANCE");
		} catch (IllegalArgumentException e) {
			return InvalidArgument.create("Enum class " + moduleName + " is missing a constant INSTANCE").asMaybe();
		}

		if (!(constant instanceof RxModule<?> rxModule))
			return InvalidArgument.create("Constant INSTANCE of enum class " + moduleName + " is not an RxModule").asMaybe();
		if (!RxModuleContract.class.isAssignableFrom(rxModule.contract()))
			return InvalidArgument.create("RxModule " + moduleName + " does not use an RxModuleContract").asMaybe();
		return Maybe.complete(rxModule);
	}

	private static final class Catalog {
		final Set<String> normalModules = new LinkedHashSet<>();
		final Map<String, OptionalModuleDescriptor> optionalModules = new LinkedHashMap<>();

		void add(RxArtifactDescriptor descriptor) throws IOException {
			for (String module : descriptor.modules())
				addNormal(module, descriptor.source());
			for (OptionalModuleDescriptor optional : descriptor.optionalModules()) {
				if (normalModules.contains(optional.module()))
					throw new IOException("Module " + optional.module() + " is both normal and optional in RX descriptors");
				OptionalModuleDescriptor previous = optionalModules.putIfAbsent(optional.module(), optional);
				if (previous != null && !previous.activationDependencies().equals(optional.activationDependencies()))
					throw new IOException("Conflicting optional RX module declarations for " + optional.module());
			}
		}

		void addLegacy(List<String> modules) throws IOException {
			for (String module : modules)
				addNormal(module, null);
		}

		private void addNormal(String module, URL source) throws IOException {
			if (optionalModules.containsKey(module))
				throw new IOException("Module " + module + " is both normal and optional in RX descriptors" + (source == null ? "" : " at " + source));
			normalModules.add(module);
		}

		boolean contains(String module) {
			return normalModules.contains(module) || optionalModules.containsKey(module);
		}
	}

	private static final class Resolver {
		private final Catalog catalog;
		private final Map<String, RxModule<?>> active = new LinkedHashMap<>();
		private final Map<RxModule<?>, Set<RxModule<?>>> dependencies = new LinkedHashMap<>();
		private final Set<String> dependenciesInspected = new LinkedHashSet<>();

		Resolver(Catalog catalog) {
			this.catalog = catalog;
		}

		LoadedRxModules resolve() throws ModuleConfigurationException {
			for (String module : catalog.normalModules)
				activate(module, false, new LinkedHashSet<>());

			boolean changed;
			do {
				changed = activateByRules();
				changed |= inspectExplicitDependencies();
			} while (changed);

			return new LoadedRxModules(List.copyOf(active.values()), immutableDependencies());
		}

		private boolean activateByRules() throws ModuleConfigurationException {
			boolean changed = false;
			for (OptionalModuleDescriptor optional : catalog.optionalModules.values())
				if (!active.containsKey(optional.module()) && !optional.activationDependencies().isEmpty()
						&& active.keySet().containsAll(optional.activationDependencies())) {
					activate(optional.module(), false, new LinkedHashSet<>());
					changed = true;
				}
			return changed;
		}

		private boolean inspectExplicitDependencies() throws ModuleConfigurationException {
			boolean changed = false;
			for (RxModule<?> module : List.copyOf(active.values())) {
				String moduleName = module.getClass().getName();
				if (!dependenciesInspected.add(moduleName))
					continue;
				for (RxModule<?> dependency : module.moduleDependencies()) {
					if (dependency == null)
						throw new ModuleConfigurationException("Module " + moduleName + " declares a null dependency");
					String dependencyName = dependency.getClass().getName();
					if (!catalog.contains(dependencyName))
						throw new ModuleConfigurationException("Module " + moduleName + " depends on " + dependencyName
								+ ", but that module is not announced by any RX artifact");
					boolean wasInactive = !active.containsKey(dependencyName);
					activate(dependencyName, true, new LinkedHashSet<>());
					addDependency(moduleName, dependencyName);
					changed |= wasInactive;
				}
			}
			return changed;
		}

		private void activate(String moduleName, boolean explicitlyRequired, Set<String> trail) throws ModuleConfigurationException {
			if (!trail.add(moduleName))
				throw new ModuleConfigurationException("Activation dependency cycle: " + String.join(" -> ", trail) + " -> " + moduleName);
			try {
				OptionalModuleDescriptor optional = catalog.optionalModules.get(moduleName);
				if (explicitlyRequired && optional != null)
					for (String dependency : optional.activationDependencies()) {
						if (!catalog.contains(dependency))
							throw new ModuleConfigurationException("Optional module " + moduleName + " requires unannounced module " + dependency);
						activate(dependency, true, trail);
					}

				if (!active.containsKey(moduleName)) {
					Maybe<RxModule<?>> maybe = loadRxModule(moduleName);
					if (maybe.isUnsatisfied())
						throw new ModuleConfigurationException("Cannot load RX module " + moduleName + ": " + maybe.whyUnsatisfied().stringify());
					active.put(moduleName, maybe.get());
				}

				if (optional != null)
					for (String dependency : optional.activationDependencies())
						if (active.containsKey(dependency))
							addDependency(moduleName, dependency);
			} finally {
				trail.remove(moduleName);
			}
		}

		private void addDependency(String moduleName, String dependencyName) {
			dependencies.computeIfAbsent(active.get(moduleName), ignored -> new LinkedHashSet<>()).add(active.get(dependencyName));
		}

		private Map<RxModule<?>, Set<RxModule<?>>> immutableDependencies() {
			Map<RxModule<?>, Set<RxModule<?>>> result = new LinkedHashMap<>();
			for (RxModule<?> module : active.values())
				result.put(module, Set.copyOf(dependencies.getOrDefault(module, Set.of())));
			return Map.copyOf(result);
		}
	}

	private static final class ModuleConfigurationException extends Exception {
		private static final long serialVersionUID = 1L;
		ModuleConfigurationException(String message) { super(message); }
	}

	private WireModuleLoader() {}
}
