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
package hiconic.rx.security.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.user.Group;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;

public interface UsersConfiguration extends GenericEntity {

	EntityType<UsersConfiguration> T = EntityTypes.T(UsersConfiguration.class);

	List<Role> getRoles();
	void setRoles(List<Role> roles);

	List<Group> getGroups();
	void setGroups(List<Group> groups);

	List<User> getUsers();
	void setUsers(List<User> users);
}
