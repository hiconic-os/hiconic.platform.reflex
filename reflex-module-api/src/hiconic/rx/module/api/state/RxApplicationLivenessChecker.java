package hiconic.rx.module.api.state;

import com.braintribe.gm.model.reason.Reason;

@FunctionalInterface
public interface RxApplicationLivenessChecker {

	/** Returns a reason in case of a fatality or null if there is no issue. */
	Reason checkLiveness();

}
