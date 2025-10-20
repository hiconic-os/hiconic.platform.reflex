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
package hiconic.rx.web.rest.wire.space;

import java.util.HashMap;
import java.util.Map;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.rest.servlet.DdraEndpointsExceptionHandler;
import hiconic.rx.web.rest.servlet.RestHandlerKey;
import hiconic.rx.web.rest.servlet.RestV2Server;
import hiconic.rx.web.rest.servlet.handlers.AbstractRestV2Handler;
import hiconic.rx.web.rest.servlet.handlers.RestV2DeleteEntitiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2DeletePropertiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2GetEntitiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2GetPropertiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2Handler;
import hiconic.rx.web.rest.servlet.handlers.RestV2OptionsHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2PatchEntitiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2PatchPropertiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2PostEntitiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2PostPropertiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2PutEntitiesHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2PutPropertiesHandler;
import hiconic.rx.web.server.api.WebServerContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class RestServerRxModuleSpace implements RxModuleContract {

	private static final String DEFAULT_MIME_TYPE = "application/json";
	
	@Import
	private RxPlatformContract platform;
	
	@Import
	private WebServerContract webServer;
	
	@Import
	private AccessContract access;
	
	@Import
	private TcSpace tc;

	@Override
	public void onDeploy() {
		webServer.addServlet("rest", "/rest/*", server());
	}
	
	@Managed
	private RestV2Server server() {
		RestV2Server bean = new RestV2Server();
		bean.setAccessDomains(access.accessDomains());
		bean.setEvaluator(platform.evaluator());
		bean.setExceptionHandler(exceptionHandler());
		bean.setHandlers(handlers());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setTraversingCriteriaMap(tc.criteriaMap());
		return bean;
	}
	
	@Managed
	private DdraEndpointsExceptionHandler exceptionHandler() {
		DdraEndpointsExceptionHandler handler = new DdraEndpointsExceptionHandler();
		handler.setIncludeDebugInformation(true);
		handler.setDefaultMarshaller(platform.marshallers().getMarshaller(DEFAULT_MIME_TYPE));
		handler.setDefaultMimeType(DEFAULT_MIME_TYPE);

		return handler;
	}
	
	
	@Managed
	public Map<RestHandlerKey, RestV2Handler<?>> handlers() {
		Map<RestHandlerKey, RestV2Handler<?>> handlers = new HashMap<>();

		// entities
		handlers.put(new RestHandlerKey("GET","entities"), getEntities());
		handlers.put(new RestHandlerKey("POST","entities"), postEntities());
		handlers.put(new RestHandlerKey("PUT","entities"), putEntities());
		handlers.put(new RestHandlerKey("PATCH","entities"), patchEntities());
		handlers.put(new RestHandlerKey("DELETE","entities"), deleteEntities());
		handlers.put(new RestHandlerKey("OPTIONS","entities"), optionsHandler());

		// properties
		handlers.put(new RestHandlerKey("GET","properties"), getProperties());
		handlers.put(new RestHandlerKey("POST","properties"), postProperties());
		handlers.put(new RestHandlerKey("PUT","properties"), putProperties());
		handlers.put(new RestHandlerKey("PATCH","properties"), patchProperties());
		handlers.put(new RestHandlerKey("DELETE","properties"), deleteProperties());
		handlers.put(new RestHandlerKey("OPTIONS","properties"), optionsHandler());

		return handlers;
	}

	@Managed
	private RestV2GetEntitiesHandler getEntities() {
		RestV2GetEntitiesHandler handler = new RestV2GetEntitiesHandler();
		handler.setAccessDomains(access.accessDomains());
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2PostEntitiesHandler postEntities() {
		RestV2PostEntitiesHandler handler = new RestV2PostEntitiesHandler();
		handler.setSessionFactory(access.systemSessionFactory());
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2PutEntitiesHandler putEntities() {
		RestV2PutEntitiesHandler handler = new RestV2PutEntitiesHandler();
		handler.setSessionFactory(access.systemSessionFactory());
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2PatchEntitiesHandler patchEntities() {
		RestV2PatchEntitiesHandler handler = new RestV2PatchEntitiesHandler();
		handler.setSessionFactory(access.systemSessionFactory());
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2DeleteEntitiesHandler deleteEntities() {
		RestV2DeleteEntitiesHandler handler = new RestV2DeleteEntitiesHandler();
		handler.setAccessDomains(access.accessDomains());
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2OptionsHandler optionsHandler() {
		RestV2OptionsHandler handler = new RestV2OptionsHandler();
		return handler;
	}

	@Managed
	private RestV2GetPropertiesHandler getProperties() {
		RestV2GetPropertiesHandler handler = new RestV2GetPropertiesHandler();
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2PostPropertiesHandler postProperties() {
		RestV2PostPropertiesHandler handler = new RestV2PostPropertiesHandler();
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2PutPropertiesHandler putProperties() {
		RestV2PutPropertiesHandler handler = new RestV2PutPropertiesHandler();
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2PatchPropertiesHandler patchProperties() {
		RestV2PatchPropertiesHandler handler = new RestV2PatchPropertiesHandler();
		configureHandlerCommons(handler);
		return handler;
	}

	@Managed
	private RestV2DeletePropertiesHandler deleteProperties() {
		RestV2DeletePropertiesHandler handler = new RestV2DeletePropertiesHandler();
		configureHandlerCommons(handler);
		return handler;
	}
	
	private void configureHandlerCommons(AbstractRestV2Handler<?> handler) {
		handler.setEvaluator(platform.evaluator());
		handler.setMarshallerRegistry(platform.marshallers());
		handler.setTraversingCriteriaMap(tc.criteriaMap());
	}

}