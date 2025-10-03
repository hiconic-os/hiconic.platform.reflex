package hiconic.rx.module.api.state;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Reason;

public interface RxApplicationStateManager {
	boolean isReady();
	
	boolean isLive();
	
	RxApplicationState getApplicationState();
	
	Reason getDeathReason();
	
	void addStateListener(Consumer<RxApplicationState> listener);

	void addLivenessChecker(Callable<Reason> checker);
}
