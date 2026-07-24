package hiconic.rx.explorer.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ExplorerConfiguration extends GenericEntity {

	EntityType<ExplorerConfiguration> T = EntityTypes.T(ExplorerConfiguration.class);

	@Initializer("[]")
	List<ModelEnvironmentConfiguration> getModelEnvironments();
	void setModelEnvironments(List<ModelEnvironmentConfiguration> modelEnvironments);

	/**
	 * Data access used for Explorer bootstrap resources when the entry-point URL does not yet carry
	 * an {@code accessId}. This is deliberately explicit: a remembered client-side access must not
	 * make the server guess which workbench supplies branding and locale.
	 */
	String getDefaultDataAccessId();
	void setDefaultDataAccessId(String defaultDataAccessId);
}
