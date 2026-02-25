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
package hiconic.rx.messaging.jdbc.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface JdbcMessagingConfiguration extends GenericEntity {

	EntityType<JdbcMessagingConfiguration> T = EntityTypes.T(JdbcMessagingConfiguration.class);

	@Initializer("'messaging'")
	String getDatabaseId();
	void setDatabaseId(String databaseId);

	@Description("Prefix for tables, indices, triggers, functions created for this messaging.\n"
			+ "Note that internally it expands the prefix with '_msg_', so that for sql prefix 'hc' the table names will be 'hc_msg_topic' and 'hc_msg_queue'.")
	@Initializer("'hc'")
	String getSqlPrefix();
	void setSqlPrefix(String sqlPrefix);

}
