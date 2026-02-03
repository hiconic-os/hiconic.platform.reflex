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
package hiconic.rx.access.hibernate.model.configuration;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;

public interface HibernateAccess extends Access, HibernatePersistenceConfiguration {

	EntityType<HibernateAccess> T = EntityTypes.T(HibernateAccess.class);

	@Mandatory
	String getDatabaseName();
	void setDatabaseName(String databaseName);

	long getDurationDebugThreshold();
	void setDurationDebugThreshold(long durationDebugThreshold);

	@Initializer("5")
	Integer getDeadlockRetryLimit();
	void setDeadlockRetryLimit(Integer deadlockRetryLimit);

	@Initializer("200")
	int getLoadingLimit();
	void setLoadingLimit(int loadingLimit);

}
