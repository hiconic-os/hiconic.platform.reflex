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
package hiconic.rx.db.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Database extends GenericEntity {

	EntityType<Database> T = EntityTypes.T(Database.class);

	String name = "name";
	String user = "user";
	String password = "password";
	
	@Mandatory
	String getName();
	void setName(String name);

	void setUser(String user);
	String getUser();

	@Confidential
	String getPassword();
	void setPassword(String password);
	
	String getDriver();
	void setDriver(String value);

	@Mandatory
	String getUrl();
	void setUrl(String value);
	
	@Initializer("60000L")
	Long getConnectionTimeout();
	void setConnectionTimeout(Long connectionTimeout);

	@Initializer("3")
	Integer getMaxPoolSize();
	void setMaxPoolSize(Integer maxPoolSize);

	@Initializer("1")
	Integer getMinPoolSize();
	void setMinPoolSize(Integer minPoolSize);

	String getPreferredTestQuery();
	void setPreferredTestQuery(String preferredTestQuery);

	Integer getMaxStatements();
	void setMaxStatements(Integer maxStatements);

	@Initializer("60000L")
	Long getIdleTimeout();
	void setIdleTimeout(Long idleTimeout);

	@Initializer("10")
	Integer getLoginTimeout();
	void setLoginTimeout(Integer loginTimeout);

	String getDataSourceName();
	void setDataSourceName(String dataSourceName);

	List<String> getInitialStatements();
	void setInitialStatements(List<String> initialStatements);

	@Initializer("true")
	boolean getEnableJmx();
	void setEnableJmx(boolean enableJmx);

	@Initializer("true")
	boolean getEnableMetrics();
	void setEnableMetrics(boolean enableMetrics);

	@Initializer("60000")
	Integer getValidationTimeout();
	void setValidationTimeout(Integer validationTimeout);

	Integer getInitializationFailTimeout();
	void setInitializationFailTimeout(Integer initializationFailTimeout);

	String getSchema();
	void setSchema(String schema);

	JdbcTransactionIsolationLevel getTransactionIsolationLevel();
	void setTransactionIsolationLevel(JdbcTransactionIsolationLevel transactionIsolationLevel);
}
