package hiconic.rx.setup.model;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.platform.reflex._ReflexSetupApiModel_;

/*package*/ class ReflexTemplateLocator {

	public static String template(String artifactId) {
		ArtifactReflection ar = _ReflexSetupApiModel_.reflection;
		return ar.groupId() + ":" + artifactId + "#" + ReflexTemplateLocator.removeRevision(ar.version());
	}

	private static String removeRevision(String version) {
		int f = version.indexOf('.');
		int l = version.lastIndexOf('.');
		if (f < 0 || l < 0 || f == l)
			throw new IllegalArgumentException("Unexpected version format. Version: " + version);
		return version.substring(0, l);
	}

}
