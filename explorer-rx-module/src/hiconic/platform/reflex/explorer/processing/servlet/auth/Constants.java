// ============================================================================
package hiconic.platform.reflex.explorer.processing.servlet.auth;

public interface Constants {

	public static String REQUEST_PARAM_USER = "user";
	public static String REQUEST_PARAM_PASSWORD = "password";
	public static String REQUEST_PARAM_CONTINUE = "continue";
	public static String REQUEST_PARAM_SESSIONID = "sessionId";
	public static String REQUEST_PARAM_SESSIONUSERICONURL = "sessionUserIconUrl";
	public static String REQUEST_PARAM_MESSAGE = "message";
	public static String REQUEST_PARAM_MESSAGESTATUS = "messageStatus";
	public static String REQUEST_PARAM_STAYSIGNED = "staySigned";

	public static String REQUEST_VALUE_MESSAGESTATUS_OK = "OK";
	public static String REQUEST_VALUE_MESSAGESTATUS_ERROR = "ERROR";

	public static String COOKIE_PREFIX = "tf";
	public static String COOKIE_SESSIONID = COOKIE_PREFIX + REQUEST_PARAM_SESSIONID;
	public static String COOKIE_USER = COOKIE_PREFIX + REQUEST_PARAM_USER;

	public static String HEADER_PARAM_PREFIX = "gm";
	public static String HEADER_PARAM_SESSIONID = HEADER_PARAM_PREFIX + "-session-id";

	public final static String TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED = "TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED";

}
