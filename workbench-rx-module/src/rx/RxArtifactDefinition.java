// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package rx;

import java.util.List;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.platform.reflex._WorkbenchRxModule_;
import hiconic.rx.module.api.wire.RxArtifact;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.workbench.module.wire.WorkbenchRxModule;

public class RxArtifactDefinition implements RxArtifact {

	@Override
	public ArtifactReflection identity() {
		return _WorkbenchRxModule_.reflection;
	}

	@Override
	public List<Class<? extends RxModule<?>>> modules() {
		return List.of(WorkbenchRxModule.class);
	}
}
