package hiconic.rx.openapi.v3.wire.space;

import com.braintribe.gm._ServiceApiModel_;
import com.braintribe.model.openapi.v3_0.api.OpenapiEntitiesRequest;
import com.braintribe.model.openapi.v3_0.api.OpenapiPropertiesRequest;
import com.braintribe.model.openapi.v3_0.api.OpenapiServicesRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.openapi.v3.processing.processor.export.AbstractOpenapiProcessor;
import hiconic.rx.openapi.v3.processing.processor.export.ApiV1OpenapiProcessor;
import hiconic.rx.openapi.v3.processing.processor.export.EntityOpenapiProcessor;
import hiconic.rx.openapi.v3.processing.processor.export.OpenapiServiceDomain;
import hiconic.rx.openapi.v3.processing.processor.export.PropertyOpenapiProcessor;
import hiconic.rx.openapi.v3.processing.servlet.OpenapiUiServlet;
import hiconic.rx.security.web.api.AuthFilters;
import hiconic.rx.web.ddra.endpoints.api.WebApiServerContract;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.webapi.endpoints.api.v1.ApiV1DdraEndpoint;
import jakarta.servlet.DispatcherType;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class OpenapiV3RxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private AccessContract access;

	@Import
	private WebApiServerContract webApiServer;

	@Import
	private WebServerContract webServer;
	
	@Override
	public void configureModels(ModelConfigurations configurations) {
		ModelConfiguration modelConfiguration = configurations.bySymbol(AbstractOpenapiProcessor.basicOpenapiProcessingModelRef);
		modelConfiguration.addModel(_ServiceApiModel_.reflection);
		modelConfiguration.addModel(ApiV1DdraEndpoint.T.getModel());
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration openapiSd = configurations.byId(OpenapiServiceDomain.openapi);
		openapiSd.bindRequest(OpenapiServicesRequest.T, this::openapiServicesProcessor);
		openapiSd.bindRequest(OpenapiEntitiesRequest.T, this::openapiEntitiesProcessor);
		openapiSd.bindRequest(OpenapiPropertiesRequest.T, this::openapiPropertiesProcessor);
	}

	@Override
	public void onDeploy() {
		webServer.addServlet("openapi-servlet", "/openapi/ui/*", openapiUiServlet());
		webServer.addFilterMapping(AuthFilters.strictAuthFilter, "/openapi/ui/*", DispatcherType.REQUEST);
	}

	@Managed
	private OpenapiUiServlet openapiUiServlet() {
		OpenapiUiServlet bean = new OpenapiUiServlet();
		bean.setServiceDomains(platform.serviceDomains());
		bean.setAccessDomains(access.accessDomains());

		// TODO configure api and rest paths, once they are configurable
		// bean.setWebApiPath("api");
		// bean.setRestPath("rest");

		return bean;
	}

	@Managed
	private ApiV1OpenapiProcessor openapiServicesProcessor() {
		ApiV1OpenapiProcessor bean = new ApiV1OpenapiProcessor();
		bean.setPublicUrl(webServer.publicUrl());
		bean.setWebApiMappingOracle(webApiServer.mappingOracle());
		bean.setWebApiServletPath(webApiServer.servletPath());
		bean.setConfiguredModels(platform.configuredModels());
		bean.setServiceDomains(platform.serviceDomains());

		return bean;
	}

	@Managed
	private EntityOpenapiProcessor openapiEntitiesProcessor() {
		EntityOpenapiProcessor bean = new EntityOpenapiProcessor();
		bean.setPublicUrl(webServer.publicUrl());
		bean.setConfiguredModels(platform.configuredModels());
		bean.setAccessDomains(access.accessDomains());

		return bean;
	}

	@Managed
	private PropertyOpenapiProcessor openapiPropertiesProcessor() {
		PropertyOpenapiProcessor bean = new PropertyOpenapiProcessor();
		bean.setPublicUrl(webServer.publicUrl());
		bean.setConfiguredModels(platform.configuredModels());
		bean.setAccessDomains(access.accessDomains());

		return bean;
	}

}