package hiconic.rx.module.api.service;

import java.util.List;

import com.braintribe.model.meta.GmMetaModel;

public interface ConfiguredModels {
	/**
	 * Returns the {@link ServiceDomain} for given domainId if available otherwise null
	 */
	ConfiguredModel byName(String modelName);
	
	ConfiguredModel byReference(ModelReference reference);
	
	ConfiguredModel mainPersistenceModel();
	
	/**
	 * Enumerates all available ServiceDomains
	 */
	List<? extends ConfiguredModel> list();
	
	List<? extends ConfiguredModel> listDependers(GmMetaModel model);
}
