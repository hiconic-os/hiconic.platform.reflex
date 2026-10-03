// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.wire.space;

import java.util.List;

import com.braintribe.gm._BasicResourceModel_;
import com.braintribe.model.resource.Resource;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.smood.model.configuration.SmoodAccess;
import hiconic.rx.model.resources.api.ModelResourcesContract;
import hiconic.rx.model.resources.processing.ModelResourceRegistry;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class ModelResourcesRxModuleSpace implements RxModuleContract, ModelResourcesContract {
	@Import private AccessContract access;

	@Override
	public Resource publish(String stableId, Resource resource) {
		return registry().publish(stableId, resource);
	}

	@Override
	public void configureModels(ModelConfigurations configurations) {
		access.configureModels(accessDenotation());
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		access.deploy(accessDenotation());
	}

	@Override
	public void onApplicationReady() {
		registry().persistAll();
	}

	@Managed
	private SmoodAccess accessDenotation() {
		SmoodAccess result = SmoodAccess.T.create();
		result.setAccessId(ACCESS_ID);
		result.setDisplayName("Model Resources");
		result.setDataModelNames(List.of(_BasicResourceModel_.name));
		return result;
	}

	@Managed
	private ModelResourceRegistry registry() {
		ModelResourceRegistry bean = new ModelResourceRegistry();
		bean.setSessionFactory(access.systemSessionFactory());
		return bean;
	}
}
