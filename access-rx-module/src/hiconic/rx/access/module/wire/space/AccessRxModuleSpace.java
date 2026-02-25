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
package hiconic.rx.access.module.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.gm._AccessApiModel_;
import com.braintribe.gm._ModelEnvironmentApiModel_;
import com.braintribe.gm._ResourceApiModel_;
import com.braintribe.gm.model.persistence.reflection.api.PersistenceReflectionRequest;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.accessapi.PersistenceRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resourceapi.persistence.ManageResource;
import com.braintribe.model.resourceapi.persistence.UploadResources;
import com.braintribe.model.resourceapi.stream.DownloadResource;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._ResourceStorageApiModel_;
import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.model.configuration.AccessConfiguration;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.access.module.api.AccessModelSymbols;
import hiconic.rx.access.module.api.AccessServiceModelConfiguration;
import hiconic.rx.access.module.api.PersistenceServiceDomain;
import hiconic.rx.access.module.processing.PersistenceReflectionProcessor;
import hiconic.rx.access.module.processing.ResourceRequestProcessor;
import hiconic.rx.access.module.processing.RxAccessModelConfigurations;
import hiconic.rx.access.module.processing.RxAccesses;
import hiconic.rx.access.module.processing.RxPersistenceGmSessionFactory;
import hiconic.rx.access.module.processing.RxPersistenceProcessor;
import hiconic.rx.access.module.processing.resource.RxResourceAccessFactory;
import hiconic.rx.access.module.processing.resource.RxResourceUrlBuilderSupplier;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxTransientDataContract;

@Managed
public class AccessRxModuleSpace implements RxModuleContract, AccessContract, AccessExpertContract, AccessModelSymbols {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private RxTransientDataContract transientData;
	
	@Import
	private RxAuthContract auth;

	@Override
	public void onDeploy() {
		AccessConfiguration accessConfiguration = getOrTunnel(platform.readConfig(AccessConfiguration.T));

		for (Access access : accessConfiguration.getAccesses())
			deploy(access);
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		// TODO why is this here and not in explorer module?
		ServiceDomainConfiguration persistenceSd = configurations.byId(PersistenceServiceDomain.persistence);

		persistenceSd.addModel(_ModelEnvironmentApiModel_.reflection); // brings ModelEnvironmentRequests...
		persistenceSd.bindRequest(PersistenceReflectionRequest.T, this::persistenceReflectionProcessor);

		accesses().initServiceDomainConfigurations(configurations);
	}

	@Override
	public void configureModels(ModelConfigurations configurations) {
		accessModelConfigurations().initModelConfigurations(configurations);

		configurePersistenceProcessor(configurations);
		configureResourceRequestProcessor(configurations);
	}

	private void configurePersistenceProcessor(ModelConfigurations configurations) {
		ModelConfiguration mc = configurations.bySymbol(configuredAccessApiModel);
		mc.addModel(_AccessApiModel_.reflection);
		mc.bindRequest(PersistenceRequest.T, this::persistenceProcessor);
	}

	private void configureResourceRequestProcessor(ModelConfigurations configurations) {
		ModelConfiguration accessApiMc = configurations.bySymbol(configuredResourceApiModel);

		AccessServiceModelConfiguration mc = accessModelConfigurations().serviceModelConfiguration(accessApiMc);
		mc.addModel(_ResourceApiModel_.reflection);
		mc.addModel(_ResourceStorageApiModel_.reflection);
		mc.bindAccessRequest(DownloadResource.T, this::resourceRequestProcessor);
		mc.bindAccessRequest(ManageResource.T, this::resourceRequestProcessor);
		mc.bindAccessRequest(UploadResources.T, this::resourceRequestProcessor);
	}

	@Managed
	private RxPersistenceProcessor persistenceProcessor() {
		RxPersistenceProcessor bean = new RxPersistenceProcessor();
		bean.setAccessDomains(accesses());
		return bean;
	}

	@Managed
	private ResourceRequestProcessor resourceRequestProcessor() {
		ResourceRequestProcessor bean = new ResourceRequestProcessor();
		bean.setSystemEvaluator(platform.systemEvaluator());
		return bean;
	}

	@Override
	@Managed
	public RxAccessModelConfigurations accessModelConfigurations() {
		RxAccessModelConfigurations bean = new RxAccessModelConfigurations();
		bean.setContextSessionFactory(contextSessionFactory());
		bean.setSystemSessionFactory(systemSessionFactory());
		return bean;
	}

	@Override
	public <A extends Access> void registerAccessExpert(EntityType<A> accessType, AccessExpert<A> expert) {
		accesses().registerExpert(accessType, expert);
	}

	@Override
	public void deploy(Access accessDenotation) {
		accesses().deploy(accessDenotation);
	}

	@Override
	public void deploy(Access accessDenotation, IncrementalAccess access) {
		accesses().deploy(accessDenotation, access);
	}

	@Override
	public AccessDomains accessDomains() {
		return accesses();
	}

	@Override
	@Managed
	public RxPersistenceGmSessionFactory contextSessionFactory() {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(AttributeContexts::peek);
		bean.setResourceAccessFactory(contextResourceAccessFactory());
		configure(bean);
		return bean;
	}

	@Override
	public RxPersistenceGmSessionFactory sessionFactory(AttributeContext attributeContext) {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(() -> attributeContext);
		bean.setResourceAccessFactory(resourceAccessFactory(attributeContext));
		configure(bean);
		return bean;
	}

	@Override
	@Managed
	public RxPersistenceGmSessionFactory systemSessionFactory() {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(platform.systemAttributeContextSupplier());
		bean.setResourceAccessFactory(systemResourceAccessFactory());
		configure(bean);
		return bean;
	}
	
	private RxResourceAccessFactory resourceAccessFactory(AttributeContext attributeContext) {
		RxResourceAccessFactory bean = new RxResourceAccessFactory();
		bean.setShallowifyRequestResource(true);
		bean.setStreamPipeFactory(transientData.streamPipeFactory());
		bean.setUrlBuilderSupplier(resourceBuilderSupplier(attributeContext));
		return bean;
	}
	
	@Managed
	private RxResourceAccessFactory contextResourceAccessFactory() {
		RxResourceAccessFactory bean = new RxResourceAccessFactory();
		bean.setShallowifyRequestResource(true);
		bean.setStreamPipeFactory(transientData.streamPipeFactory());
		bean.setUrlBuilderSupplier(contextResourceBuilderSupplier());
		return bean;
	}
	@Managed
	private RxResourceAccessFactory systemResourceAccessFactory() {
		RxResourceAccessFactory bean = new RxResourceAccessFactory();
		bean.setShallowifyRequestResource(true);
		bean.setStreamPipeFactory(transientData.streamPipeFactory());
		bean.setUrlBuilderSupplier(systemResourceBuilderSupplier());
		return bean;
	}
	
	@Managed
	private RxResourceUrlBuilderSupplier contextResourceBuilderSupplier() {
		var bean = new RxResourceUrlBuilderSupplier();
		
		bean.setSessionIdProvider(auth.contextUserSessionIdSupplier());
		bean.setBaseStreamingUrl(streamingUrl());
		bean.setResponseMimeType("application/json");
		
		return bean;
	}
	
	@Managed
	private RxResourceUrlBuilderSupplier resourceBuilderSupplier(AttributeContext attributeContext) {
		var bean = new RxResourceUrlBuilderSupplier();
		
		bean.setSessionIdProvider(auth.userSessionIdSupplier(attributeContext));
		bean.setBaseStreamingUrl(streamingUrl());
		bean.setResponseMimeType("application/json");
		
		return bean;
	}
	
	@Managed
	private RxResourceUrlBuilderSupplier systemResourceBuilderSupplier() {
		var bean = new RxResourceUrlBuilderSupplier();
		
		bean.setSessionIdProvider(auth.systemUserSessionIdSupplier());
		bean.setBaseStreamingUrl(streamingUrl());
		bean.setResponseMimeType("application/json");
		
		return bean;
	}
	
	// TODO: needs to handled by the web-api extension which needs to communicate its knowledge to this layer (HOW?)
	private URL streamingUrl() {
		try {
			return URI.create("http://localhost:8080/services/streaming").toURL();
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Managed
	public PersistenceReflectionProcessor persistenceReflectionProcessor() {
		PersistenceReflectionProcessor bean = new PersistenceReflectionProcessor();
		bean.setAccesses(accesses());
		return bean;
	}

	private void configure(RxPersistenceGmSessionFactory bean) {
		bean.setAccesses(accesses());
		bean.setEvaluatorSupplier(platform::evaluator);
	}

	@Managed
	private RxAccesses accesses() {
		RxAccesses bean = new RxAccesses();
		bean.setConfiguredModels(platform.configuredModels());
		bean.setServiceDomains(platform.serviceDomains());
		bean.setContextSessionFactory(contextSessionFactory());
		bean.setSystemSessionFactory(systemSessionFactory());
		bean.setAccessModelConfigurations(accessModelConfigurations());
		return bean;
	}
}