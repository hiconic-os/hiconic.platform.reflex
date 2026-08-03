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
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.user.User;

/** Users which are idempotently ensured in the configured persistent {@code UserService} during application startup. */
public interface UserProvisioningConfiguration extends GenericEntity {

	EntityType<UserProvisioningConfiguration> T = EntityTypes.T(UserProvisioningConfiguration.class);

	List<User> getUsers();
	void setUsers(List<User> users);

	/**
	 * Stable technical group used to identify users owned by this provisioning source.
	 */
	@Initializer("'provisioned.default'")
	String getProvisioningGroup();
	void setProvisioningGroup(String provisioningGroup);

	/**
	 * Optional, explicitly changed revision which authorizes a one-time reconciliation of existing credentials.
	 * Normal profile, role and group reconciliation is independent of this revision.
	 */
	String getReconciliationRevision();
	void setReconciliationRevision(String reconciliationRevision);

	/** Whether users owned by {@link #getProvisioningGroup()} but absent from {@link #getUsers()} are deleted. */
	boolean getDeleteVanishedUsers();
	void setDeleteVanishedUsers(boolean deleteVanishedUsers);
}
