package hiconic.rx.access.hibernate.model.configuration;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.access.model.configuration.Access;

public interface HibernateAccess extends Access {
	EntityType<HibernateAccess> T = EntityTypes.T(HibernateAccess.class);

	String getDatabaseName();
	void setDatabaseName(String databaseName);
}
