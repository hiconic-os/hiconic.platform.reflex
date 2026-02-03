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
package hiconic.rx.hibernate.model.configuration;

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface HibernatePersistenceConfiguration extends GenericEntity {

	EntityType<HibernatePersistenceConfiguration> T = EntityTypes.T(HibernatePersistenceConfiguration.class);

	Integer getMappingVersion();
	void setMappingVersion(Integer mappingVersion);

	Map<String, String> getProperties();
	void setProperties(Map<String, String> value);

	String getDefaultSchema();
	void setDefaultSchema(String defaultSchema);

	String getDefaultCatalog();
	void setDefaultCatalog(String defaultCatalog);

	String getObjectNamePrefix();
	void setObjectNamePrefix(String objectNamePrefix);

	String getTableNamePrefix();
	void setTableNamePrefix(String tableNamePrefix);

	String getForeignKeyNamePrefix();
	void setForeignKeyNamePrefix(String foreignKeyNamePrefix);

	String getUniqueKeyNamePrefix();
	void setUniqueKeyNamePrefix(String uniqueKeyNamePrefix);

	String getIndexNamePrefix();
	void setIndexNamePrefix(String indexNamePrefix);

	/** If true, sets Hibernate's Environment.SHOW_SQL property to true. */
	boolean getShowSql();
	void setShowSql(boolean showSql);

}
