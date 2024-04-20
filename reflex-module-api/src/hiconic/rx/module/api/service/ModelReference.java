package hiconic.rx.module.api.service;

import com.braintribe.common.artifact.ArtifactReflection;

@FunctionalInterface
public interface ModelReference {
	
	String modelName();
	
	static ModelReference configured(ArtifactReflection artifactReflection) {
		return prefixed("configured", artifactReflection);
	}
	
	static ModelReference prefixed(String prefix, ArtifactReflection artifactReflection) {
		return of(artifactReflection.groupId() + ":" + prefix + "-" + artifactReflection.artifactId());
	}
	
	static ModelReference of(String name) {
		return () -> name;
	};
	
}
