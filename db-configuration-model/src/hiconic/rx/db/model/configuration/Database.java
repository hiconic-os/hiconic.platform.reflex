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
	
	@Mandatory
	String getDriver();
	void setDriver(String value);

	@Mandatory
	String getUrl();
	void setUrl(String value);
	
	@Initializer("60000")
	Integer getCheckoutTimeout();
	void setCheckoutTimeout(Integer checkoutTimeout);

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

	@Initializer("60000")
	Integer getMaxIdleTime();
	void setMaxIdleTime(Integer maxIdleTime);

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
