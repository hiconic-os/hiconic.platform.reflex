package hiconic.rx.locking.jdbc.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.braintribe.common.concurrent.ScheduledTask;
import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.locking.db.impl.DbLocking;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.scope.InstanceConfiguration;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.locking.jdbc.model.configuration.JdbcLockingConfiguration;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.messaging.api.MessagingDestinationsContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Provides {@link DbLocking} implementation for {@link LockingContract}
 */
@Managed
public class JdbcLockingRxModuleSpace implements RxModuleContract, LockingContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private DatabaseContract database;

	@Import
	private MessagingContract messaging;

	@Import
	@SuppressWarnings("deprecation")
	private MessagingDestinationsContract messagingDestinations;

	private static final com.braintribe.logging.Logger log = com.braintribe.logging.Logger.getLogger(JdbcLockingRxModuleSpace.class);

	@Override
	@Managed
	public DbLocking locking() {
		JdbcLockingConfiguration configuration = configuration();

		int lockExpirationInSecs = resolveLockExpiration(configuration);

		DbLocking bean = new DbLocking();
		// DB
		bean.setAutoUpdateSchema(configuration.getAutoUpdateSchema());
		bean.setDataSource(dataSource(configuration));
		// Locking
		bean.setLockExpirationInSecs(lockExpirationInSecs);
		bean.setPollIntervalInMillies(configuration.getPollIntervalInMillis());
		// Messaging
		bean.setMessagingSessionProvider(messaging.sessionProvider()::provideMessagingSession);
		bean.setTopicExpiration(configuration.getTopicExpirationInMillis());
		bean.setTopicName(unlockTopicName());

		configureLockRefreshing(bean, InstanceConfiguration.currentInstance(), lockExpirationInSecs);

		return bean;
	}

	private void configureLockRefreshing(DbLocking bean, InstanceConfiguration instanceConfiguration, int lockExpirationInSecs) {
		int refreshSecs = lockExpirationInSecs / 3;

		TaskScheduler scheduler = platform.taskScheduler();
		ScheduledTask task = scheduler
				.scheduleAtFixedRate("db-locking-refresher", bean::refreshLockedLocks, refreshSecs, refreshSecs, TimeUnit.SECONDS).done();

		instanceConfiguration.onDestroy(() -> {
			task.cancel();
			// Await termination?
		});
	}

	private int resolveLockExpiration(JdbcLockingConfiguration deployable) {
		int lockExpirationInSecs = deployable.getLockExpirationInSecs();
		if (lockExpirationInSecs >= 10)
			return lockExpirationInSecs;

		log.warn("DbLocking has its lockExpiration set to [" + lockExpirationInSecs
				+ "] seconds, but minimum value is 10, which will be used instead.");
		return 10;
	}

	private DataSource dataSource(JdbcLockingConfiguration configuration) {
		Maybe<DataSource> dataSourceMaybe = database.dataSource(configuration.getDatabaseId());
		return getOrTunnel(dataSourceMaybe);
	}

	@SuppressWarnings("deprecation")
	private String unlockTopicName() {
		return messagingDestinations.unlockTopicName();
	}

	@Managed
	private JdbcLockingConfiguration configuration() {
		return getOrTunnel(platform.readConfig(JdbcLockingConfiguration.T));
	}

}