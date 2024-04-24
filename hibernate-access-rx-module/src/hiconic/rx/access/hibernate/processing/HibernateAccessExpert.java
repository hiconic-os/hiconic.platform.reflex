package hiconic.rx.access.hibernate.processing;

import java.util.UUID;

import javax.sql.DataSource;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.generic.processing.IdGenerator;
import com.braintribe.model.processing.core.expert.impl.ConfigurableGmExpertRegistry;

import hiconic.rx.access.hibernate.model.configuration.HibernateAccess;
import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.module.api.service.ConfiguredModel;

public class HibernateAccessExpert implements AccessExpert<HibernateAccess> {
	private HibernateContract hibernateContract;
	private DatabaseContract databaseContract;
	
	@Required
	public void setHibernateContract(HibernateContract hibernateContract) {
		this.hibernateContract = hibernateContract;
	}
	
	@Required
	public void setDatabaseContract(DatabaseContract databaseContract) {
		this.databaseContract = databaseContract;
	}

	@Override
	public Maybe<IncrementalAccess> deploy(HibernateAccess access, ConfiguredModel dataModel) {
		com.braintribe.model.access.hibernate.HibernateAccess incrementalAccess = new com.braintribe.model.access.hibernate.HibernateAccess();

		String databaseName = access.getDatabaseName();

		if (databaseName == null)
			return Reasons.build(ConfigurationError.T) //
					.text("HibernateAccess with id '" + access.getId() + "' is invalid. HibernateAccess.databaseName must not be null.") //
					.toMaybe();

		Maybe<DataSource> dataSourceMaybe = databaseContract.dataSource(databaseName);

		if (dataSourceMaybe.isUnsatisfied())
			return Reasons.build(ConfigurationError.T) //
					.text("DataSource of HibernateAccess with id '" + access.getId() + "' is not resolvable") //
					.cause(dataSourceMaybe.whyUnsatisfied()) //
					.toMaybe();

		DataSource dataSource = dataSourceMaybe.get();

		HibernatePersistence persistence = hibernateContract.persistence(dataModel.systemCmdResolver(), dataSource);

		incrementalAccess.setAccessId(access.getAccessId());
		incrementalAccess.setHibernateSessionFactory(persistence.sessionFactory());
		incrementalAccess.setModelSupplier(() -> dataModel.modelOracle().getGmMetaModel());
		incrementalAccess.setLoadingLimit(access.getLoadingLimit());
		incrementalAccess.setDeadlockRetryLimit(access.getDeadlockRetryLimit());
		incrementalAccess.setDurationDebugThreshold(access.getDurationDebugThreshold());

		ConfigurableGmExpertRegistry expertRegistry = new ConfigurableGmExpertRegistry();
		expertRegistry.add(IdGenerator.class, String.class, e -> UUID.randomUUID().toString());

		incrementalAccess.setExpertRegistry(expertRegistry);

		// TODO: model logging in configuration model
		// incrementalAccess.setLogging(null);
		return Maybe.complete(incrementalAccess);
	}
}
