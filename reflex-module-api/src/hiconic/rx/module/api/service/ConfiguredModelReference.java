package hiconic.rx.module.api.service;

import com.braintribe.common.artifact.ArtifactReflection;

@FunctionalInterface
public interface ConfiguredModelReference {
	String modelName();
	
	static ConfiguredModelReference of(ArtifactReflection artifactReflection) {
		return of("configured", artifactReflection);
	}
	
	static ConfiguredModelReference of(String prefix, ArtifactReflection artifactReflection) {
		return of(artifactReflection.groupId() + ":" + prefix + "-" + artifactReflection.artifactId());
	}
	
	static ConfiguredModelReference of(String name) {
		return () -> name;
	};
	
	
}
