package hiconic.rx.platform.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.braintribe.cfg.Required;
import com.braintribe.model.generic.eval.Evaluator;
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

	private ConfigurableDispatchingServiceProcessor fallbackProcessor;
	private Evaluator<ServiceRequest> contextEvaluator;
	private ExecutorService executorService;
	private RxModelConfigurations modelConfigurations;

	private final Map<String, RxServiceDomain> domains = new ConcurrentHashMap<>();
	private final LazyInitialized<Map<GmMetaModel, List<RxServiceDomain>>> domainsByModel = new LazyInitialized<>(this::indexDependers);

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
	public RxServiceDomain main() {
		return byId("main");
	}

	@Override
	public RxServiceDomain byId(String domainId) {
		return domains.get(domainId);
	}

	@Override
	public List<? extends ServiceDomain> listDependers(GmMetaModel model) {
		return domainsByModel.get().getOrDefault(model, Collections.emptyList());
	}

	private Map<GmMetaModel, List<RxServiceDomain>> indexDependers() {
		Map<GmMetaModel, List<RxServiceDomain>> index = new HashMap<>();

		for (RxServiceDomain domain : list()) {
			domain.modelOracle().getDependencies() //
					.includeSelf() //
					.transitive() //
					.asGmMetaModels() //
					.forEach(m -> index.computeIfAbsent(m, k -> new ArrayList<>()).add(domain));
		}
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
