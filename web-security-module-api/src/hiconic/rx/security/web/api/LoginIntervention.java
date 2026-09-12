package hiconic.rx.security.web.api;

/**
 * Declarative presentation of an authentication failure. No extension-specific code is executed in the browser.
 */
public final class LoginIntervention {
	private final LoginInterventionState state;
	private final String title;
	private final String message;
	private final String actionId;
	private final String actionLabel;

	private LoginIntervention(LoginInterventionState state, String title, String message, String actionId, String actionLabel) {
		this.state = state;
		this.title = title;
		this.message = message;
		this.actionId = actionId;
		this.actionLabel = actionLabel;
	}

	public static LoginIntervention actionRequired(String title, String message, String actionId, String actionLabel) {
		return new LoginIntervention(LoginInterventionState.ACTION_REQUIRED, title, message, actionId, actionLabel);
	}

	public static LoginIntervention pending(String title, String message) {
		return new LoginIntervention(LoginInterventionState.PENDING, title, message, null, null);
	}

	public static LoginIntervention retryAuthentication() {
		return new LoginIntervention(LoginInterventionState.RETRY_AUTHENTICATION, null, null, null, null);
	}

	public LoginInterventionState state() { return state; }
	public String title() { return title; }
	public String message() { return message; }
	public String actionId() { return actionId; }
	public String actionLabel() { return actionLabel; }
}
