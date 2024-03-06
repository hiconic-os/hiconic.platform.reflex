// ============================================================================
package hiconic.rx.security.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.user.User;

public interface UsersConfiguration extends GenericEntity {

	EntityType<UsersConfiguration> T = EntityTypes.T(UsersConfiguration.class);

	List<User> getUsers();
	void setUsers(List<User> users);
}
