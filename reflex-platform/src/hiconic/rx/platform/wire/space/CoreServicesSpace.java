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
package hiconic.rx.platform.wire.space;

import static com.braintribe.wire.api.util.Sets.set;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.braintribe.codec.marshaller.common.BasicConfigurableMarshallerRegistry;
import com.braintribe.codec.marshaller.jse.JseMarshaller;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.codec.marshaller.stax.StaxMarshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.platform.models.RxCmdResolverManager;
import hiconic.rx.platform.models.RxConfiguredModels;
import hiconic.rx.platform.models.RxModelConfigurations;
import hiconic.rx.platform.service.ContextualizingServiceRequestEvaluator;
import hiconic.rx.platform.service.RxServiceDomainDispatcher;
import hiconic.rx.platform.service.RxServiceDomains;
import hiconic.rx.platform.wire.contract.CoreServicesContract;

@Managed
public class CoreServicesSpace implements CoreServicesContract {

	@Override
	@Managed
	public RxServiceDomains serviceDomains() {
		RxServiceDomains bean = new RxServiceDomains();
		bean.setContextEvaluator(evaluator());
		bean.setFallbackProcessor(fallbackProcessor());
		bean.setExecutorService(executorService());
		bean.setModelConfigurations(modelConfigurations());
		return bean;
	}

	@Override
	@Managed
	public RxConfiguredModels configuredModels() {
		RxConfiguredModels bean = new RxConfiguredModels();
		bean.setCmdResolverManager(cmdResolverManager());
		bean.setSystemAttributeContextSupplier(systemAttributeContextSupplier());
		return bean;
	}

	@Override
	@Managed
	public Supplier<AttributeContext> systemAttributeContextSupplier() {
		return () -> AttributeContexts.derivePeek().set(UserSessionAspect.class, systemUserSession()).build();
	}

	@Managed
	protected UserSession systemUserSession() {
		UserSession bean = UserSession.T.create();

		User user = systemUser();

		Set<String> effectiveRoles = new HashSet<>();
		effectiveRoles.add("$all");
		effectiveRoles.add("$user-" + user.getName());
		for (Role userRole : user.getRoles())
			effectiveRoles.add(userRole.getName());

		Date now = new Date();

		bean.setSessionId(UUID.randomUUID().toString());
		bean.setType(UserSessionType.internal);
		bean.setCreationInternetAddress("0:0:0:0:0:0:0:1");
		bean.setCreationDate(now);
		bean.setLastAccessedDate(now);
		bean.setUser(user);
		bean.setEffectiveRoles(effectiveRoles);

		return bean;
	}

	private static final Set<String> internalRoles = set("internal");
	private static final String internalName = "internal";

	@Managed
	protected User systemUser() {

		User bean = User.T.create();
		bean.setId(internalName);
		bean.setName(internalName);

		for (String internalRoleName : internalRoles) {
			Role internalRole = Role.T.create();
			internalRole.setId(internalRoleName);
			internalRole.setName(internalRoleName);
			bean.getRoles().add(internalRole);
		}

		return bean;
	}

	@Managed
	protected RxCmdResolverManager cmdResolverManager() {
		RxCmdResolverManager bean = new RxCmdResolverManager();
		return bean;
	}

	@Managed
	public RxModelConfigurations modelConfigurations() {
		RxModelConfigurations bean = new RxModelConfigurations();
		bean.setConfiguredModels(configuredModels());
		return bean;
	}

	@Managed
	@Override
	public BasicConfigurableMarshallerRegistry marshallers() {
		BasicConfigurableMarshallerRegistry bean = new BasicConfigurableMarshallerRegistry();
		bean.registerMarshaller("application/json", jsonMarshaller());
		bean.registerMarshaller("text/yaml", yamlMarshaller());
		bean.registerMarshaller("application/yaml", yamlMarshaller());
		bean.registerMarshaller("gm/jse", jseMarshaller());
		bean.registerMarshaller("gm/xml", xmlMarshaller());
		return bean;
	}

	@Managed
	protected StaxMarshaller xmlMarshaller() {
		StaxMarshaller bean = new StaxMarshaller();
		return bean;
	}
	
	@Managed 
	protected JseMarshaller jseMarshaller() {
		JseMarshaller bean = new JseMarshaller();
		return bean;
	}
	
	@Managed
	protected JsonStreamMarshaller jsonMarshaller() {
		JsonStreamMarshaller bean = new JsonStreamMarshaller();
		bean.setUseBufferingDecoder(true);
		return bean;
	}

	@Managed
	protected YamlMarshaller yamlMarshaller() {
		return new YamlMarshaller();
	}

	@Override
	@Managed
	public ConfigurableServiceRequestEvaluator evaluator() {
		ConfigurableServiceRequestEvaluator bean = new ConfigurableServiceRequestEvaluator();
		bean.setExecutorService(executorService());
		bean.setServiceProcessor(rootServiceProcessor());
		return bean;
	}

	@Override
	public ContextualizingServiceRequestEvaluator evaluator(AttributeContext attributeContext) {
		return evaluator(() -> attributeContext);
	}

	@Override
	@Managed
	public ContextualizingServiceRequestEvaluator systemEvaluator() {
		return evaluator(systemAttributeContextSupplier());
	}

	protected ContextualizingServiceRequestEvaluator evaluator(Supplier<AttributeContext> attributeContextSupplier) {
		ContextualizingServiceRequestEvaluator bean = new ContextualizingServiceRequestEvaluator();
		bean.setDelegate(evaluator());
		bean.setAttributeContextProvider(attributeContextSupplier);
		return bean;
	}

	@Managed
	public ConfigurableDispatchingServiceProcessor rootServiceProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();

		bean.register(ServiceRequest.T, serviceDomainDispatcher());
		bean.registerInterceptor("domain-validation").register(serviceDomainDispatcher());

		return bean;
	}

	@Managed
	protected RxServiceDomainDispatcher serviceDomainDispatcher() {
		RxServiceDomainDispatcher bean = new RxServiceDomainDispatcher();
		bean.setServiceDomains(serviceDomains());
		return bean;
	}

	@Managed
	public ConfigurableDispatchingServiceProcessor fallbackProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();
		return bean;
	}

	@Override
	@Managed
	public ExecutorService executorService() {
		return Executors.newCachedThreadPool();
	}

}
