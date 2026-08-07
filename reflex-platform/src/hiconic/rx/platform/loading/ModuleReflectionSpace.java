// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.loading;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.wire.ModuleReflectionContract;

final class ModuleReflectionSpace implements ModuleReflectionContract {

	private final ArtifactReflection artifactReflection;
	private final ClassLoader moduleClassLoader;

	ModuleReflectionSpace(ArtifactReflection artifactReflection, ClassLoader moduleClassLoader) {
		this.artifactReflection = artifactReflection;
		this.moduleClassLoader = moduleClassLoader;
	}

	@Override
	public ArtifactReflection artifactReflection() {
		return artifactReflection;
	}

	@Override
	public ClassLoader moduleClassLoader() {
		return moduleClassLoader;
	}

}
