package hiconic.rx.explorer.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.check.api.CheckContract;
import hiconic.rx.check.api.CheckProcessorRegistry;
import hiconic.rx.check.model.bundle.aspect.CheckCoverage;
import hiconic.rx.check.model.bundle.aspect.CheckLatency;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.explorer.processing.check.base.BaseConnectivityCheckProcessor;
import hiconic.rx.explorer.processing.check.base.BaseFunctionalityCheckProcessor;
import hiconic.rx.explorer.processing.check.base.BaseVitalityCheckProcessor;
import hiconic.rx.explorer.processing.check.hw.MemoryCheckRxProcessor;
import hiconic.rx.explorer.processing.check.jdbc.DatabaseConnectionsCheck;
import hiconic.rx.leadership.api.LeadershipContract;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * @author peter.gazdik
 */
@Managed
public class ChecksSpace implements WireSpace {

	// @formatter:off
	@Import private PlatformReflectionSpace platformReflection;

	@Import private CheckContract check;
	@Import private DatabaseContract database;
	@Import private RxPlatformContract platform;
	@Import private LockingContract locking;
	@Import private LeadershipContract leadership;
	@Import private MessagingContract messaging;
	// @formatter:on

	public void registerChecks() {
		CheckProcessorRegistry registry = check.checkProcessorRegistry();

		registry.registerProcessor(BaseCheck.baseFunctionality, baseFunctionalityCheckProcessor(), CheckCoverage.functional, CheckLatency.moderate);
		registry.registerProcessor(BaseCheck.baseConnectivity, baseConnectivityCheckProcessor(), CheckCoverage.connectivity, CheckLatency.moderate);
		registry.registerProcessor(BaseCheck.baseVitality, baseVitalityCheckProcessor(), CheckCoverage.vitality, CheckLatency.moderate);

		registry.registerProcessor(BaseCheck.baseDatabaseConnection, databaseConnectionCheck(), CheckCoverage.connectivity, CheckLatency.moderate);
		registry.registerProcessor(BaseCheck.baseMemory, memoryCheckProcessor(), CheckCoverage.vitality, CheckLatency.moderate);
	}

	@Managed
	private DatabaseConnectionsCheck databaseConnectionCheck() {
		DatabaseConnectionsCheck bean = new DatabaseConnectionsCheck();
		// bean.setTimeWarnThreshold(getRuntimeLongValue("TRIBEFIRE_CHECK_JDBC_CONN_WARN"));
		bean.setDatabaseContract(database);
		return bean;
	}

	@Managed
	private MemoryCheckRxProcessor memoryCheckProcessor() {
		MemoryCheckRxProcessor bean = new MemoryCheckRxProcessor();
		// TODO make MemoryCheckRxProcessor thresholds configurable
		// bean.setGlobalMemoryAvailableWarnThreshold(getRuntimeValue("TRIBEFIRE_CHECK_MEMORY_GLOBAL_WARN"));
		// bean.setGlobalMemoryAvailableFailThreshold(getRuntimeValue("TRIBEFIRE_CHECK_MEMORY_GLOBAL_FAIL"));
		// bean.setSwapAvailableWarnThreshold(getRuntimeValue("TRIBEFIRE_CHECK_MEMORY_SWAP_WARN"));
		// bean.setSwapAvailableFailThreshold(getRuntimeValue("TRIBEFIRE_CHECK_MEMORY_SWAP_FAIL"));
		// bean.setJavaMemoryAvailableWarnThreshold(getRuntimeValue("TRIBEFIRE_CHECK_MEMORY_JAVA_WARN"));
		// bean.setJavaMemoryAvailableFailThreshold(getRuntimeValue("TRIBEFIRE_CHECK_MEMORY_JAVA_FAIL"));
		return bean;
	}

	@Managed
	private BaseFunctionalityCheckProcessor baseFunctionalityCheckProcessor() {
		BaseFunctionalityCheckProcessor bean = new BaseFunctionalityCheckProcessor();

		// TODO review checking locking/leadership - was only set in Cortex based on ENV variables
		bean.setLocking(locking.locking());
		bean.setLeadershipManager(leadership.leadershipManager());
		bean.setScheduledExecutorService(platform.scheduledExecutorService());
		bean.setRequestEvaluator(platform.evaluator());
		return bean;
	}

	@Managed
	private BaseConnectivityCheckProcessor baseConnectivityCheckProcessor() {
		BaseConnectivityCheckProcessor bean = new BaseConnectivityCheckProcessor();
		bean.setScheduledExecutorService(platform.scheduledExecutorService());
		bean.setMessagingSessionProviderSupplier(() -> messaging.sessionProvider());
		bean.setDatabaseInformationProvider(platformReflection.databaseInformationProvider());
		return bean;
	}

	@Managed
	private BaseVitalityCheckProcessor baseVitalityCheckProcessor() {
		BaseVitalityCheckProcessor bean = new BaseVitalityCheckProcessor();
		return bean;
	}

}
