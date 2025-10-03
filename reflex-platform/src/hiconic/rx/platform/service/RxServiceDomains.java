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

import static com.braintribe.utils.lcd.CollectionTools2.acquireList;
import static java.util.Collections.emptyList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.platform.reflex._ReflexPlatform_;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.platform.models.RxConfiguredModel;
import hiconic.rx.platform.models.RxModelConfigurations;

public class RxServiceDomains implements ServiceDomains {

	private static final Logger log = Logger.getLogger(RxServiceDomains.class);

	private ConfigurableDispatchingServiceProcessor fallbackProcessor;
	private Evaluator<ServiceRequest> contextEvaluator;
	private ExecutorService executorService;
	private RxModelConfigurations modelConfigurations;

	private final Map<String, RxServiceDomain> domains = new ConcurrentHashMap<>();
	private final LazyInitialized<Map<GmMetaModel, List<RxServiceDomain>>> domainsByModel = new LazyInitialized<>(this::indexGmModelToDomains);
	private final LazyInitialized<Map<EntityType<?>, List<RxServiceDomain>>> domainsByReqType = new LazyInitialized<>(this::indexReqTypeToDomains);

	@Required
	public void setModelConfigurations(RxModelConfigurations modelConfigurations) {
		this.modelConfigurations = modelConfigurations;
	}

	@Required
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	@Required
	public void setContextEvaluator(Evaluator<ServiceRequest> contextEvaluator) {
		this.contextEvaluator = contextEvaluator;
	}

	@Required
	public void setFallbackProcessor(ConfigurableDispatchingServiceProcessor fallbackProcessor) {
		this.fallbackProcessor = fallbackProcessor;
	}

	@Override
	public RxServiceDomain byId(String domainId) {
		return domains.get(domainId);
	}

	@Override
	public List<? extends ServiceDomain> listDomains(EntityType<? extends ServiceRequest> requestType) {
		Model model = requestType.getModel();
		if (model != null)
			return listDomains(model.<GmMetaModel> getMetaModel());
		else
			return domainsByReqType.get().getOrDefault(requestType, emptyList());
	}

	@Override
	public List<? extends ServiceDomain> listDomains(GmMetaModel model) {
		return domainsByModel.get().getOrDefault(model, emptyList());
	}

	private Map<GmMetaModel, List<RxServiceDomain>> indexGmModelToDomains() {
		Map<GmMetaModel, List<RxServiceDomain>> index = new IdentityHashMap<>();

		for (RxServiceDomain domain : list()) {
			if (log.isDebugEnabled())
				logModelsForDomain(domain);

			domain.modelOracle().getDependencies() //
					.includeSelf() //
					.transitive() //
					.asGmMetaModels() //
					.forEach(gmModel -> acquireList(index, gmModel).add(domain));
		}

		return index;
	}

	private void logModelsForDomain(RxServiceDomain domain) {
		String s = domain.modelOracle().getDependencies() //
				.includeSelf() //
				.transitive() //
				.asGmMetaModels() //
				.map(GmMetaModel::getName) //
				.collect(Collectors.joining("\n\t", "Models for domain [" + domain.domainId() + "]\n\t", ""));

		log.debug(s);
	}

	private Map<EntityType<?>, List<RxServiceDomain>> indexReqTypeToDomains() {
		Map<EntityType<?>, List<RxServiceDomain>> index = new IdentityHashMap<>();

		for (RxServiceDomain domain : list())
			domain.modelOracle().getEntityTypeOracle(ServiceRequest.T) //
					.getSubTypes() //
					.transitive() //
					.onlyInstantiable() //
					.asTypes() //
					.forEach(reqType -> acquireList(index, (EntityType<?>) reqType).add(domain));

		return index;
	}

	public RxServiceDomain acquire(String domainId) {
		return domains.computeIfAbsent(domainId, this::createServiceDomain);
	}

	private RxServiceDomain createServiceDomain(String domainId) {
		RxConfiguredModel configuredModel = modelConfigurations.byName(_ReflexPlatform_.groupId + ":configured-" + domainId + "-api-model");

		return new RxServiceDomain(domainId, configuredModel, executorService, contextEvaluator, fallbackProcessor);
	}

	@Override
	public List<RxServiceDomain> list() {
		return List.copyOf(domains.values());
	}
}
