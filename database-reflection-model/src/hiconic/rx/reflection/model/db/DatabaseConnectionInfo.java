// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.reflection.model.db;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface DatabaseConnectionInfo extends GenericEntity {

	EntityType<DatabaseConnectionInfo> T = EntityTypes.T(DatabaseConnectionInfo.class);

	String getName();
	void setName(String name);

	String getUrl();
	void setUrl(String url);

	Long getConnectionTimeout();
	void setConnectionTimeout(Long connectionTimeout);

	Long getIdleTimeout();
	void setIdleTimeout(Long idleTimeout);

	Long getMaxLifetime();
	void setMaxLifetime(Long maxLifetime);

	Integer getMinimumPoolSize();
	void setMinimumPoolSize(Integer minimumPoolSize);

	Integer getMaximumPoolSize();
	void setMaximumPoolSize(Integer maximumPoolSize);

	DatabaseConnectionPoolMetrics getMetrics();
	void setMetrics(DatabaseConnectionPoolMetrics metrics);
}
