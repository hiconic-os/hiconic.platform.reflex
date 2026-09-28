// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.module.wire.space;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	public void associateWorkbench(String dataAccessId, String workbenchAccessId) {
		if (dataAccessId == null || dataAccessId.isBlank())
			throw new IllegalArgumentException("Data access id must not be blank");
		if (workbenchAccessId == null || workbenchAccessId.isBlank())
			throw new IllegalArgumentException("Workbench access id must not be blank");

		String previous = workbenchAccessIds().putIfAbsent(dataAccessId, workbenchAccessId);
		if (previous != null && !previous.equals(workbenchAccessId))
			throw new IllegalStateException("Data access '" + dataAccessId + "' is already associated with workbench access '"
					+ previous + "' and cannot also be associated with '" + workbenchAccessId + "'");
	}

	@Override
	public String workbenchAccessId(String dataAccessId) {
		return workbenchAccessIds().get(dataAccessId);
	}

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
	private Map<String, String> workbenchAccessIds() {
		return new ConcurrentHashMap<>();
	}

	@Managed
	private WorkbenchInitializers initializers() {
		WorkbenchInitializers bean = new WorkbenchInitializers();
		bean.setSessionFactory(access.systemSessionFactory());
		bean.setStandardInitializerIdentity(WorkbenchInitializerIdentity.wire(moduleReflection, InstanceConfiguration.currentInstance().qualification()));
		return bean;
	}

}
