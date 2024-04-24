package hiconic.rx.access.hibernate.model.configuration;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.access.model.configuration.Access;

public interface HibernateAccess extends Access {
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
