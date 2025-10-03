package hiconic.rx.leadership.lockingbased.wire.space;

import java.util.concurrent.TimeUnit;

import com.braintribe.common.concurrent.ScheduledTask;
import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.scope.InstanceConfiguration;

import hiconic.rx.leadership.api.LeadershipContract;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import tribefire.cortex.leadership.api.LeadershipManager;
import tribefire.cortex.leadership.impl.LockingBasedLeadershipManager;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class LeadershipViaLockingRxModuleSpace implements RxModuleContract, LeadershipContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private LockingContract locking;

	private static int LEADERSHIP_REFRESH_INTERVAL_MS = 10_000;

	@Override
	@Managed
	public LeadershipManager leadershipManager() {
		LockingBasedLeadershipManager bean = new LockingBasedLeadershipManager();
		bean.setLocking(locking.locking());
		bean.setName("Rx Leadership Manager");

		configureLeadershipRefresh(bean, InstanceConfiguration.currentInstance());

		return bean;
	}

	private void configureLeadershipRefresh(LockingBasedLeadershipManager leadershipManager, InstanceConfiguration instanceConfiguration) {
		TaskScheduler scheduler = platform.taskScheduler();

		ScheduledTask task = scheduler.scheduleAtFixedRate( //
				"leadership-refresher", //
				() -> leadershipManager.refreshLeadershipsForEligibleDomains(), //
				LEADERSHIP_REFRESH_INTERVAL_MS, LEADERSHIP_REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS) //
				.done();

		instanceConfiguration.onDestroy(() -> {
			task.cancel();
			// Await termination?
		});
	}

}