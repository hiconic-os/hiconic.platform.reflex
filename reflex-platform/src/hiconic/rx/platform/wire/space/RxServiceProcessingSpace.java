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

import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.module.api.wire.RxExecutionContract;
import hiconic.rx.module.api.wire.RxServiceProcessingContract;
import hiconic.rx.platform.service.ContextualizingServiceRequestEvaluator;
import hiconic.rx.platform.service.RxServiceDomainDispatcher;
import hiconic.rx.platform.service.RxServiceDomains;

@Managed
public class RxServiceProcessingSpace implements RxServiceProcessingContract {

	@Import
	private RxExecutionContract execution;

	@Import
	private RxAuthContract auth;

	@Import
	private RxConfigurationSpace configuration;

	@Override
	@Managed
	public RxServiceDomains serviceDomains() {
		RxServiceDomains bean = new RxServiceDomains();
		bean.setContextEvaluator(evaluator());
		bean.setFallbackProcessor(fallbackProcessor());
		bean.setExecutorService(execution.executorService());
		bean.setModelConfigurations(configuration.modelConfigurations());
		return bean;
	}

	@Override
	@Managed
	public ConfigurableServiceRequestEvaluator evaluator() {
		ConfigurableServiceRequestEvaluator bean = new ConfigurableServiceRequestEvaluator();
		bean.setExecutorService(execution.executorService());
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
		return evaluator(auth.systemAttributeContextSupplier());
	}

	private ContextualizingServiceRequestEvaluator evaluator(Supplier<AttributeContext> attributeContextSupplier) {
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
	private RxServiceDomainDispatcher serviceDomainDispatcher() {
		RxServiceDomainDispatcher bean = new RxServiceDomainDispatcher();
		bean.setServiceDomains(serviceDomains());
		return bean;
	}

	@Managed
	public ConfigurableDispatchingServiceProcessor fallbackProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();
		return bean;
	}

}
