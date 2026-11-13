// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.platform.loading;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.wire.OptionalRxModule;
import hiconic.rx.module.api.wire.RxArtifact;
import hiconic.rx.module.api.wire.RxModule;

/** Loads each fixed-name definition in its own class loader to avoid name collisions. */
final class RxArtifactDefinitionReader {

	static final String RESOURCE = "rx/RxArtifactDefinition.class";
	private static final String CLASS_NAME = "rx.RxArtifactDefinition";

	record OptionalModuleDescriptor(String module, List<String> activationDependencies) {}
	record RxArtifactDescriptor(ArtifactReflection identity, List<ArtifactReflection> dependencies,
		List<String> modules, List<OptionalModuleDescriptor> optionalModules, URL source) {}

	static RxArtifactDescriptor read(URL url) throws IOException {
		try {
			byte[] bytes;
			try (var input = url.openStream()) {
				bytes = input.readAllBytes();
			}
			Class<?> definitionClass = new DefinitionClassLoader(RxArtifact.class.getClassLoader()).define(bytes);
			if (!RxArtifact.class.isAssignableFrom(definitionClass))
				throw new IOException("RX artifact definition does not implement RxArtifact: " + url);
			RxArtifact definition = (RxArtifact) definitionClass.getDeclaredConstructor().newInstance();
			ArtifactReflection identity = Objects.requireNonNull(definition.identity(), "RX artifact identity");
			List<ArtifactReflection> dependencies = List.copyOf(definition.dependencies());
			List<String> modules = definition.modules().stream().map(Class::getName).toList();
			List<OptionalModuleDescriptor> optionalModules = definition.optionalModules().stream()
					.map(RxArtifactDefinitionReader::optionalDescriptor).toList();
			return new RxArtifactDescriptor(identity, dependencies, modules, optionalModules, url);
		} catch (IOException e) {
			throw e;
		} catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
			throw new IOException("Could not load RX artifact definition from " + url, e);
		}
	}

	private static OptionalModuleDescriptor optionalDescriptor(OptionalRxModule optional) {
		return new OptionalModuleDescriptor(optional.module().getName(),
				optional.activationDependencies().stream().map(Class::getName).toList());
	}

	private static final class DefinitionClassLoader extends ClassLoader {
		DefinitionClassLoader(ClassLoader parent) {
			super(parent);
		}

		Class<?> define(byte[] bytes) {
			return defineClass(CLASS_NAME, bytes, 0, bytes.length);
		}
	}

	private RxArtifactDefinitionReader() {}
}
