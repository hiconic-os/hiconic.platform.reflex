// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.module.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.scope.InstanceConfiguration;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.module.api.wire.ModuleReflectionContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.workbench.api.WorkbenchContract;
import hiconic.rx.workbench.api.WorkbenchInitializer;
import hiconic.rx.workbench.api.WorkbenchInitializerIdentity;
import hiconic.rx.workbench.api.WorkbenchInitializerRegistration;
import hiconic.rx.workbench.processing.WorkbenchInitializers;

@Managed
public class WorkbenchRxModuleSpace implements RxModuleContract, WorkbenchContract {

	@Import
	private AccessContract access;
	@Import
	private ModuleReflectionContract moduleReflection;

	@Override
	public void registerInitializer(String workbenchAccessId, WorkbenchInitializer initializer) {
		initializers().register(workbenchAccessId, initializer);
	}

	@Override
	public void registerInitializer(String workbenchAccessId, WorkbenchInitializerRegistration registration) {
		initializers().register(workbenchAccessId, registration);
	}

	@Override
	public void onApplicationReady() {
		initializers().initializeAll();
	}

	@Managed
	private WorkbenchInitializers initializers() {
		WorkbenchInitializers bean = new WorkbenchInitializers();
		bean.setSessionFactory(access.systemSessionFactory());
		bean.setStandardInitializerIdentity(WorkbenchInitializerIdentity.wire(moduleReflection, InstanceConfiguration.currentInstance().qualification()));
		return bean;
	}

}
