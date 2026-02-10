// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.module.wire.space;

import java.io.File;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ProcessorRegistry;
import com.braintribe.model.processing.service.common.CompositeServiceProcessor;
import com.braintribe.model.processing.service.common.UnicastProcessor;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.model.service.api.CompositeRequest;
import com.braintribe.model.service.api.UnicastRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.platform.resource.FsResourceStorage;
import hiconic.rx.platform.resource.ResourcePayloadProcessor;
import hiconic.rx.platform.resource.RxResourcesStorages;
import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;
import hiconic.rx.resource.model.api.ResourcePayloadRequest;
import hiconic.rx.resource.model.configuration.FileSystemResourceStorage;
import hiconic.rx.resource.model.configuration.ResourceStorageConfiguration;

/**
 * Module that brings core ...
 */
@Managed
public class CoreRxPlatformModuleSpace implements RxModuleContract {

	// @formatter:off
	@Import private ExtendedRxPlatformContract platform;
	// @formatter:on

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configureSystemDomain(configurations);
	}

	// ###############################################
	// ## . . . . . System Service Domain . . . . . ##
	// ###############################################

	private void configureSystemDomain(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration systemSd = configurations.system();
		systemSd.bindRequest(CompositeRequest.T, this::compositeProcessor);
		systemSd.bindRequest(UnicastRequest.T, this::unicastProcessor);

	}

	@Managed
	private UnicastProcessor unicastProcessor() {
		UnicastProcessor bean = new UnicastProcessor();
		bean.setCurrentInstance(platform.instanceId());
		return bean;
	}

	@Managed
	private CompositeServiceProcessor compositeProcessor() {
		CompositeServiceProcessor bean = new CompositeServiceProcessor();
		// TODO configure "swallowed" exceptions log level

		return bean;
	}

	// ###############################################
	// ## . . . . . . Fallback Processors . . . . . ##
	// ###############################################

	@Override
	public void registerFallbackProcessors(ProcessorRegistry processorRegistry) {
		processorRegistry.register(ResourcePayloadRequest.T, resourceDownloadProcessor());
	}

	@Managed
	private ResourcePayloadProcessor resourceDownloadProcessor() {
		ResourcePayloadProcessor bean = new ResourcePayloadProcessor();
		bean.setServiceDomains(platform.serviceDomains());
		bean.setResourceStorages(platform.resourceStorages());

		return bean;
	}

	// ###############################################
	// ## . . . . . .Configure Platform . . . . . . ##
	// ###############################################

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		configurator.registerResourceStorageDeploymentExpert(FileSystemResourceStorage.T, FileSystemSource.T, this::deployFsResourceStorage);
	}

	private Maybe<ResourceStorage> deployFsResourceStorage(FileSystemResourceStorage storageDenotation) {
		FsResourceStorage result = new FsResourceStorage();
		result.setStorageId(storageDenotation.getStorageId());
		result.setBaseDir(new File(storageDenotation.getBaseDir()));

		return Maybe.complete(result);
	}

	@Override
	public void onDeploy() {
		configureResourceStorages();
	}

	private void configureResourceStorages() {
		ResourceStorageConfiguration rsConfig = platform.readConfig(ResourceStorageConfiguration.T).get();

		RxResourcesStorages resourceStorages = platform.resourceStorages();

		for (hiconic.rx.resource.model.configuration.ResourceStorage storage : rsConfig.getStorages())
			resourceStorages.deployLazy(storage);
	}

}
