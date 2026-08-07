// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.module.api.wire;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.wire.api.space.WireSpace;

/**
 * Reflection of the RX module owning the current module Wire context.
 * <p>
 * The platform binds a dedicated instance for every loaded module. Importing this contract from a module therefore always reflects that module,
 * analogous to the classic Tribefire module reflection contract.
 */
public interface ModuleReflectionContract extends WireSpace {

	ArtifactReflection artifactReflection();

	ClassLoader moduleClassLoader();

	default String artifactId() {
		return artifactReflection().artifactId();
	}

	default String groupId() {
		return artifactReflection().groupId();
	}

	default String version() {
		return artifactReflection().version();
	}

	default String moduleName() {
		return artifactReflection().name();
	}

	default String globalId() {
		return "module://" + moduleName();
	}

}
