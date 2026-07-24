package hiconic.rx.explorer.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Associates the access-specific parts which form an Explorer model environment. */
public interface ModelEnvironmentConfiguration extends GenericEntity {

	EntityType<ModelEnvironmentConfiguration> T = EntityTypes.T(ModelEnvironmentConfiguration.class);

	String getDataAccessId();
	void setDataAccessId(String dataAccessId);

	String getWorkbenchAccessId();
	void setWorkbenchAccessId(String workbenchAccessId);
}
