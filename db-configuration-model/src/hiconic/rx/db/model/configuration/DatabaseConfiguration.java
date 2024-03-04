// ============================================================================
package hiconic.rx.db.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface DatabaseConfiguration extends GenericEntity {

	EntityType<DatabaseConfiguration> T = EntityTypes.T(DatabaseConfiguration.class);

	List<Database> getDatabases();
	void setDatabases(List<Database> databases);
}
