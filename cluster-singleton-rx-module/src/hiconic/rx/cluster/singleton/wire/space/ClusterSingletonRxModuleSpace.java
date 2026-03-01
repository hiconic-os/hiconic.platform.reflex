package hiconic.rx.cluster.singleton.wire.space;

import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.lock.impl.SimpleCdlLocking;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.messaging.dmb.wire.space.DmbMessagingRxModuleSpace;
import hiconic.rx.leadership.api.LeadershipContract;
import hiconic.rx.leadership.lockingbased.wire.space.LeadershipViaLockingRxModuleSpace;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Binds {@link LockingContract}.
 * <p>
 * {@link LeadershipContract} is bound by {@link LeadershipViaLockingRxModuleSpace}.
 * <p>
 * {@link MessagingContract} is bound by {@link DmbMessagingRxModuleSpace}.
 */
@Managed
public class ClusterSingletonRxModuleSpace implements RxModuleContract, LockingContract {

	@Import
	private RxPlatformContract platform;

	@Override
	@Managed
	public Locking locking() {
		Locking bean = new SimpleCdlLocking();

		return bean;
	}

}