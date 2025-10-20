package hiconic.rx.explorer.wire.space;

import java.util.function.Supplier;

import com.braintribe.utils.stream.pools.CompoundBlockPool;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.check.api.CheckContract;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.explorer.processing.platformreflection.PlatformReflectionProcessor;
import hiconic.rx.explorer.processing.platformreflection.application.StandardRxAppInfoProvider;
import hiconic.rx.explorer.processing.platformreflection.db.StandardDatabaseInformationProvider;
import hiconic.rx.explorer.processing.platformreflection.system.StandardSystemInformationProvider;
import hiconic.rx.leadership.api.LeadershipContract;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.module.api.wire.RxTransientDataContract;
import hiconic.rx.reflection.model.application.RxAppInfo;
import hiconic.rx.security.api.SecurityContract;

/**
 * @author peter.gazdik
 */
@Managed
public class PlatformReflectionSpace implements WireSpace {

	// @formatter:off
	@Import private RxPlatformContract platform;
	@Import private RxPlatformResourcesContract platformResources;
	@Import private RxTransientDataContract transientData;
	
	@Import private CheckContract check;
	@Import private DatabaseContract database;
	@Import private LockingContract locking;
	@Import private LeadershipContract leadership;
	@Import private MessagingContract messaging;
	@Import private SecurityContract security;

	@Import private SystemToolsSpace systemTools;
	// @formatter:on

	@Managed
	public PlatformReflectionProcessor platformReflectionProcessor() {
		PlatformReflectionProcessor bean = new PlatformReflectionProcessor();
		bean.setApplicationId(platform.applicationId());
		bean.setInstanceId(platform.instanceId());
		bean.setAllowedRoles(security.adminAndInternalRoles());
		bean.setSystemInformationProvider(systemInformationProvider());
		bean.setRxAppInfoProvider(rxAppInfoProvider());
		bean.setCommandExecution(systemTools.commandExecution());
		bean.setStreamPipeFactory(transientData.streamPipeFactory());
		bean.setJsonMarshaller(platform.jsonMarshaller());
		bean.setCheckResultMarshaller(check.checkResultToHtmlMarshaller());
		
		// TODO make zip password configurable
		bean.setZipPassword(null);
		bean.setConfFolder(platformResources.confPath().toFile());
		return bean;
	}

	@Managed
	private StandardSystemInformationProvider systemInformationProvider() {
		StandardSystemInformationProvider bean = new StandardSystemInformationProvider();
		bean.setMessagingSessionProviderSupplier(() -> messaging.sessionProvider());
		bean.setDatabaseInformationProvider(databaseInformationProvider());
		bean.setLocking(locking.locking());
		bean.setLeadershipManager(leadership.leadershipManager());
		return bean;
	}

	// TODO extract elsewhere?
	// used to be platform-reflection-processing for impl
	// bean was SystemInformationSpace.databaseInformationProvider
	@Managed
	public StandardDatabaseInformationProvider databaseInformationProvider() {
		StandardDatabaseInformationProvider bean = new StandardDatabaseInformationProvider();
		bean.setDatabases(database.allDatabases());
		return bean;
	}

	@Managed
	private Supplier<RxAppInfo> rxAppInfoProvider() {
		StandardRxAppInfoProvider bean = new StandardRxAppInfoProvider();
		bean.setPlatformContract(platform);
		bean.setPlatformResourcesContract(platformResources);
		if (transientData.streamPipeFactory() instanceof CompoundBlockPool blockPool)
			bean.setCompoundBlockPool(blockPool);
		return bean;
	}

}
