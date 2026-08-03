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
package hiconic.rx.platform.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.DelegatingModelConfiguration;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.util.DisplayNames;
import hiconic.rx.platform.models.RxConfiguredModel;

public class RxServiceDomain implements ServiceDomain, ServiceDomainConfiguration, DelegatingModelConfiguration {

	private final String domainId;
	private final RxServiceDomains serviceDomains;
	private volatile String displayName;
	private final ConfigurableServiceRequestEvaluator evaluator;

	private final RxConfiguredModel modelConfiguration;
	private final RxRequestDispatcher dispatcher;
	
	private final List<Supplier<? extends ServiceRequest>> defaultRequestSuppliers = Collections.synchronizedList(new ArrayList<>());
	private final Set<String> allowedRoles = Collections.synchronizedSet(new HashSet<>());
	
	public RxServiceDomain(String domainId, RxServiceDomains serviceDomains, RxConfiguredModel modelConfiguration, ExecutorService executorService,
			Evaluator<ServiceRequest> contextEvaluator, ServiceProcessor<ServiceRequest, Object> fallbackProcessor) {
		this.domainId = domainId;
		this.serviceDomains = serviceDomains;
		this.modelConfiguration = modelConfiguration;
		
		dispatcher = new RxRequestDispatcher();
		dispatcher.setServiceDomain(this);
		dispatcher.setFallbackProcessor(fallbackProcessor);
		
		evaluator = new ConfigurableServiceRequestEvaluator();
		evaluator.setExecutorService(executorService);
		evaluator.setServiceProcessor(dispatcher);
		evaluator.setContextEvaluator(contextEvaluator);
	}
	
	@Override
	public ModelConfiguration modelConfiguration() {
		return modelConfiguration;
	}
	
	@Override
	public ConfiguredModel configuredModel() {
		return modelConfiguration;
	}
	
	@Override
	public String domainId() {
		return domainId;
	}

	@Override
	public String displayName() {
		String result = displayName;
		return result == null || result.isBlank() ? DisplayNames.fromTechnicalName(domainId) : result;
	}

	@Override
	public Set<String> aliases() {
		return serviceDomains.aliasesOf(this);
	}

	@Override
	public void addAlias(String alias) {
		serviceDomains.registerAlias(alias, this);
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public Set<String> allowedRoles() {
		synchronized (allowedRoles) {
			return Set.copyOf(allowedRoles);
		}
	}

	@Override
	public void allowRoles(Collection<String> roles) {
		if (roles == null)
			return;

		for (String role : roles)
			if (role != null && !role.isBlank())
				allowedRoles.add(role);
	}

	@Override
	public CmdResolver systemCmdResolver() {
		return modelConfiguration.systemCmdResolver();
	}
	
	@Override
	public CmdResolver contextCmdResolver() {
		return modelConfiguration.contextCmdResolver();
	}
	
	@Override
	public CmdResolver cmdResolver(AttributeContext attributeContext) {
		return modelConfiguration.cmdResolver(attributeContext);
	}

	@Override
	public ModelOracle modelOracle() {
		return systemCmdResolver().getModelOracle();
	}

	@Override
	public Evaluator<ServiceRequest> evaluator() {
		return evaluator;
	}
	
	@Override
	public ServiceRequest defaultRequest() {
		for (Supplier<? extends ServiceRequest> supplier: defaultRequestSuppliers) {
			ServiceRequest request = supplier.get();
			if (request != null)
				return request;
		}
		
		return null;
	}
	
	@Override
	public void addDefaultRequestSupplier(Supplier<? extends ServiceRequest> serviceRequestSupplier) {
		this.defaultRequestSuppliers.add(serviceRequestSupplier);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + domainId + "]";
	}

}	
