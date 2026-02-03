package hiconic.rx.module.api.state;

import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Reason;

public interface RxApplicationStateManager {
	/**
	 * Returns true if the application has been bootstrapped to the point where endpoints are available.
	 */
	boolean isReady();

	/**
	 * Returns true if the application is still alive, determined via checkers registered with
	 * {@link #addLivenessChecker(RxApplicationLivenessChecker)}.
	 */
	boolean isLive();

	/** Returns the current state of the application. */
	RxApplicationState getState();

	/**
	 * Returns a reason why the {@link RxApplicationStateManager#getState() application state} is {@link RxApplicationState#dead}, if relevant, or
	 * null.
	 */
	Reason getDeathReason();

	/** Registers a listener for {@link #getState() application state} changes. */
	void addStateChangeListener(Consumer<RxApplicationState> listener);

	/**
	 * Registers a checker that contributes to the overall determination of the liveness of the application.
	 * <p>
	 * If no checker reports an issue the application is alive, otherwise it is dead afterwards.
	 */
	void addLivenessChecker(RxApplicationLivenessChecker checker);
}
