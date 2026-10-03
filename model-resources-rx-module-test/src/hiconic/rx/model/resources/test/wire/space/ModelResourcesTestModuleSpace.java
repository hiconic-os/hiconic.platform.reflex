// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.test.wire.space;

import com.braintribe.model.resource.Resource;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.model.resources.api.ModelResourcesContract;
import hiconic.rx.model.resources.test.wire.contract.ModelResourcesTestContract;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class ModelResourcesTestModuleSpace implements RxModuleContract, ModelResourcesTestContract {
	private static final String RESOURCE_ID = "test.model-resource";

	@Import private RxPlatformContract platform;
	@Import private ModelResourcesContract modelResources;

	private Resource reference;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		Resource resource = platform.packagedResources().resolver().resource("test-resources/assets/model-resource.txt")
				.withMimeType().withFileSize().asResource();
		reference = modelResources.publish(RESOURCE_ID, resource);
	}

	@Override
	public Resource reference() {
		return reference;
	}
}
