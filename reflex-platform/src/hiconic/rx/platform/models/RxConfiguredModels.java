package hiconic.rx.platform.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ConfiguredModelReference;
import hiconic.rx.module.api.service.ConfiguredModels;

public class RxConfiguredModels implements ConfiguredModels {
	private Map<String, RxConfiguredModel> models = new ConcurrentHashMap<>();
	private LazyInitialized<Map<GmMetaModel, List<RxConfiguredModel>>> dependersByModel = new LazyInitialized<>(this::indexDependers);

	@Override
	public RxConfiguredModel byName(String domainId) {
		return models.get(domainId);
	}
	
	public RxConfiguredModel acquire(String modelName) {
		return models.computeIfAbsent(modelName, n -> new RxConfiguredModel(this, n));
	}
	
	public RxConfiguredModel acquire(ConfiguredModelReference modelReference) {
		return acquire(modelReference.modelName());
	}

	@Override
	public List<RxConfiguredModel> list() {
		return List.copyOf(models.values());
	}

	private Map<GmMetaModel, List<RxConfiguredModel>> indexDependers() {
		Map<GmMetaModel, List<RxConfiguredModel>> index = new HashMap<>();

		for (RxConfiguredModel configuredModel: list()) {
			configuredModel.modelOracle().getDependencies() //
				.includeSelf() //
				.transitive() //
				.asGmMetaModels() //
				.forEach(m -> index.computeIfAbsent(m, k -> new ArrayList<>()).add(configuredModel));
		}
		return index;
	}
	
	@Override
	public List<? extends ConfiguredModel> listDependers(GmMetaModel model) {
		return dependersByModel.get().getOrDefault(model, Collections.emptyList());
	}

}
