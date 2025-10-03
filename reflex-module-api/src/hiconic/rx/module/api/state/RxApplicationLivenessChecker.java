package hiconic.rx.module.api.state;

import com.braintribe.gm.model.reason.Reason;

@FunctionalInterface
public interface RxApplicationLivenessChecker {
	/** Returns a reason in case of a fatality that describes the fatality otherwise null which implies liveness */
	Reason checkLiveness();
}
