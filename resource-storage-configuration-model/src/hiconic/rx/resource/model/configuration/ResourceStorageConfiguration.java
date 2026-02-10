// ============================================================================
package hiconic.rx.resource.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ResourceStorageConfiguration extends GenericEntity {

	EntityType<ResourceStorageConfiguration> T = EntityTypes.T(ResourceStorageConfiguration.class);

	List<ResourceStorage> getStorages();
	void setStorages(List<ResourceStorage> storages);

}
