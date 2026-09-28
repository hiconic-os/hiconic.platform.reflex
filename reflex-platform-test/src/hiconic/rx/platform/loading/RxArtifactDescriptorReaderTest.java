// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.platform.loading;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import hiconic.rx.workbench.module.wire.WorkbenchRxModule;

public class RxArtifactDescriptorReaderTest {

	@Test
	public void readsFixedPackageDescriptorWithoutLoadingIt() throws Exception {
		var resources = getClass().getClassLoader().getResources("rx/package-info.class");
		assertThat(resources.hasMoreElements()).isTrue();

		var descriptor = RxArtifactDescriptorReader.read(resources.nextElement());

		assertThat(descriptor.modules()).contains(WorkbenchRxModule.class.getName());
		assertThat(descriptor.optionalModules()).isEmpty();
	}

	@Test
	public void combinesNewAndLegacyDescriptors() {
		var modules = WireModuleLoader.loadWireModules();

		assertThat(modules.isSatisfied()).isTrue();
		assertThat(modules.get().modules()).contains(WorkbenchRxModule.INSTANCE);
	}
}
