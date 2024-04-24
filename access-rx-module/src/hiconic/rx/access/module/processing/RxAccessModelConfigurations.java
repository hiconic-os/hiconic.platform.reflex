package hiconic.rx.access.module.processing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import hiconic.rx.access.module.api.AccessModelConfiguration;
import hiconic.rx.access.module.api.AccessModelConfigurations;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ModelReference;

public class RxAccessModelConfigurations implements AccessModelConfigurations {
	private ModelConfigurations modelConfigurations;
	private Map<ModelConfiguration, RxAccessModelConfiguration> accessModelConfigurations = new ConcurrentHashMap<>();
	
	public void initModelConfigurations(ModelConfigurations modelConfigurations) {
		this.modelConfigurations = modelConfigurations;
	}
	
	@Override
	public AccessModelConfiguration byConfiguration(ModelConfiguration modelConfiguration) {
		return accessModelConfigurations.computeIfAbsent(modelConfiguration, RxAccessModelConfiguration::new);
	}
	
	@Override
	public AccessModelConfiguration byName(String modeName) {
		return byConfiguration(modelConfigurations.byName(modeName));
	}
	
	@Override
	public AccessModelConfiguration byReference(ModelReference modelReference) {
		return byConfiguration(modelConfigurations.byReference(modelReference));
	}
}
