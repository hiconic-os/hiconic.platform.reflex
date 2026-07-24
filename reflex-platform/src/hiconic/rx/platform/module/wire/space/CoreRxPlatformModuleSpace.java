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
import java.util.HashSet;
import java.util.Set;

import com.braintribe.gm.model.logging.level.api.LogLevelRequest;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ProcessorRegistry;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.CompositeServiceProcessor;
import com.braintribe.model.processing.service.common.UnicastProcessor;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.model.service.api.AuthorizedRequest;
import com.braintribe.model.service.api.CompositeRequest;
import com.braintribe.model.service.api.InternalPushRequest;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.PushRequest;
import com.braintribe.model.service.api.UnicastRequest;
import com.braintribe.model.service.api.result.PushResponse;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ModelSymbol;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.processing.auth.RoleAuthorizationInterceptor;
import hiconic.rx.platform.processing.cluster.SingleInstanceMulticastProcessor;
import hiconic.rx.platform.processing.push.PushChannelLifecycleHub;
import hiconic.rx.platform.processing.push.PushProcessor;
import hiconic.rx.platform.resource.FsResourceStorage;
import hiconic.rx.platform.resource.ResourcePayloadProcessor;
import hiconic.rx.platform.resource.RxResourcesStorages;
import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;
import hiconic.rx.push.api.PushChannel;
import hiconic.rx.push.api.PushChannelLifecyclePublisher;
import hiconic.rx.push.api.PushContract;
import hiconic.rx.resource.model.api.ResourcePayloadRequest;
import hiconic.rx.resource.model.configuration.FileSystemResourceStorage;
import hiconic.rx.resource.model.configuration.ResourceStorageConfiguration;

/**
 * Module that brings core ...
 */
@Managed
public class CoreRxPlatformModuleSpace implements RxModuleContract, PushContract {

	private static ModelSymbol internalDomainDefaultingApiModelSymbol = ModelSymbol.of("internal-domain-defaulting-api-model");
	
	// @formatter:off
	@Import private ExtendedRxPlatformContract platform;
	@Import private LogLevelSpace logLevels;
	// @formatter:on


	@Override
	public void configureModels(ModelConfigurations configurations) {
		ModelConfiguration defaultingModelConfiguration = configurations.bySymbol(internalDomainDefaultingApiModelSymbol);
		configureInternalDomainDefaulting(defaultingModelConfiguration);
	}

	private void configureInternalDomainDefaulting(ModelConfiguration internalDefaultingModelConfiguration) {
		internalDefaultingModelConfiguration.bindRequest(MulticastRequest.T, this::defaultMulticastProcessor);
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configurations.main().setDisplayName("Main");
		configureInternalDomain(configurations);
		configureSystemDomain(configurations);
		configureLoggingDomain(configurations);
	}

	// ################################################
	// ## . . . . Internal Service Domain . . . . . . ##
	// ################################################

	private void configureInternalDomain(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration internalSd = configurations.internal();
		internalSd.setDisplayName("Internal");
		internalSd.allowRoles(Set.of("internal"));
		internalSd.bindRequest(UnicastRequest.T, this::unicastProcessor);
		internalSd.bindRequest(PushRequest.T, this::pushProcessor);
		internalSd.bindRequest(InternalPushRequest.T, this::pushProcessor);
		internalSd.addModel(internalDomainDefaultingApiModelSymbol);
	}

	@Managed
	private PushProcessor pushProcessor() {
		PushProcessor bean = new PushProcessor();
		bean.setTargetApplicationId(platform.application().instanceId().getApplicationId());
		return bean;
	}

	@Override
	public void addHandler(ServiceProcessor<? super InternalPushRequest, PushResponse> handler) {
		pushProcessor().addHandler(handler);
	}

	@Override
	public PushChannelLifecyclePublisher channelLifecyclePublisher() {
		return pushChannelLifecycleHub();
	}

	@Override
	public void registerChannel(PushChannel channel) {
		pushChannelLifecycleHub().notifyConnectionEstablished(channel);
	}

	@Override
	public void unregisterChannel(PushChannel channel) {
		pushChannelLifecycleHub().notifyConnectionClosed(channel);
	}

	@Managed
	private PushChannelLifecycleHub pushChannelLifecycleHub() {
		return new PushChannelLifecycleHub();
	}

	// ###############################################
	// ## . . . . . System Service Domain . . . . . ##
	// ###############################################

	private void configureSystemDomain(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration systemSd = configurations.system();
		systemSd.setDisplayName("System");
		systemSd.bindRequest(CompositeRequest.T, this::compositeProcessor);
	}
	
	private void configureLoggingDomain(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration loggingSd = configurations.byId("logging");
		loggingSd.setDisplayName("Logging");
		loggingSd.addModel(LogLevelRequest.T.getModel());
		loggingSd.bindInterceptor("role-authorization").forType(AuthorizedRequest.T).bind(this::logLevelAuthorizationInterceptor);
		loggingSd.bindRequest(LogLevelRequest.T, logLevels::logLevelProcessor);
	}

	@Managed
	private RoleAuthorizationInterceptor logLevelAuthorizationInterceptor() {
		RoleAuthorizationInterceptor bean = new RoleAuthorizationInterceptor();
		bean.setRoleAuthorization(platform.auth().roleAuthorization());
		bean.setRoles(logLevelAuthorizedRoles());
		bean.setMessage("Insufficient privileges to manage log levels");
		return bean;
	}

	private Set<String> logLevelAuthorizedRoles() {
		Set<String> roles = new HashSet<>(platform.auth().roleAuthorization().adminRoles());
		roles.add("internal");
		return roles;
	}

	@Managed
	private UnicastProcessor unicastProcessor() {
		UnicastProcessor bean = new UnicastProcessor();
		bean.setCurrentInstance(platform.application().instanceId());
		return bean;
	}
	
	@Managed
	private SingleInstanceMulticastProcessor defaultMulticastProcessor() {
		SingleInstanceMulticastProcessor bean = new SingleInstanceMulticastProcessor();
		bean.setInstanceId(platform.application().instanceId());
		bean.setRequestEvaluator(platform.serviceProcessing().evaluator());
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
		bean.setServiceDomains(platform.serviceProcessing().serviceDomains());
		bean.setResourceStorages(platform.resourceStorages());
		bean.setPackagedResourceResolvers(platform.packagedResources(), platform.packagedPublicResources());

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
		ResourceStorageConfiguration rsConfig = platform.configuration().readConfig(ResourceStorageConfiguration.T).get();

		RxResourcesStorages resourceStorages = platform.resourceStorages();

		for (hiconic.rx.resource.model.configuration.ResourceStorage storage : rsConfig.getStorages())
			resourceStorages.deployLazy(storage);
	}

}
