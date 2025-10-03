package hiconic.rx.module.api.state;

import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Reason;

public interface RxApplicationStateManager {
	/**
	 * Returns true if the application has been bootstrapped to the point where endpoints are available.
	 */
	boolean isReady();
	
	/**
	 * Returns true if the application is still alive which is verified with the checkers registered with
	 * {@link #addLivenessChecker(RxApplicationLivenessChecker)}.
	 */
	boolean isLive();
	
	/**
	 * Returns the current state of the application. 
	 */
	RxApplicationState getState();
	
	/**
	 * Returns a reason if the {@link RxApplicationStateManager#getState() application state} is {@link RxApplicationState#dead dead}
	 * describing why the application is considered in this state otherwise null.
	 */
	Reason getDeathReason();
	
	/**
	 * Registers a listener to be notified about each change of the {@link #getState() application state}.
	 */
	void addStateChangeListener(Consumer<RxApplicationState> listener);

	/**
	 * Registers a checker that contributes to the overall determination of the liveness of the application. 
	 * If a liveness checker returns null it means there is no issue found by this checker. If all checkers
	 * return null the application if alive otherwise it is dead afterwards.
	 */
	void addLivenessChecker(RxApplicationLivenessChecker checker);
}
