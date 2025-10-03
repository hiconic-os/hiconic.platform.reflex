package hiconic.rx.module.api.state;

public enum RxApplicationState {
	/** The application has not yet started the code that loads and initializes modules and itself. */
	initial,
	/** The application is about to load and initialize itself and all of its modules. */
	starting,
	/** The application has loaded and initialized itself and its modules and readily exposed all endpoints. */
	started,
	/** The application is in the process of sanely shutting down. */
	stopping,
	/** The application was sanely shutdown. */
	stopped,
	/** The application is detected to be in a fatal state and should no longer be used. This state is determined with
	 * {@link RxApplicationStateManager#addLivenessChecker(RxApplicationLivenessChecker) registered liveness checkers}. */
	dead,
}
