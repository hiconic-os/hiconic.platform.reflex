// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.module.api.wire;

import java.util.List;

import com.braintribe.common.artifact.ArtifactReflection;

/** The identity, dependencies and RX modules contributed by one artifact. */
public interface RxArtifact {

	ArtifactReflection identity();

	default List<ArtifactReflection> dependencies() {
		return List.of();
	}

	default List<Class<? extends RxModule<?>>> modules() {
		return List.of();
	}

	default List<OptionalRxModule> optionalModules() {
		return List.of();
	}
}
