// ============================================================================
package hiconic.rx.explorer.processing.check.jdbc;

public class ExceptionUtil {

	private static final String EXCEPTION_SUFIX = "Exception:";
	
	private ExceptionUtil() {
		//private constructor to ensure only static usage of this class.
	}
	
	public static String getLastMessage(Throwable throwable) {
		if (throwable == null)
			return null;
		
		String message = throwable.getMessage();
		while ((throwable = throwable.getCause()) != null) {
			if (throwable.getMessage() != null) {
				message = throwable.getMessage();
			}
		}
		
		if (message != null) {
			int index = message.indexOf(EXCEPTION_SUFIX);
			if (index != -1) {
				String initialPart = message.substring(0, index);
				if (!initialPart.contains(" ") && initialPart.length() > EXCEPTION_SUFIX.length()) {
					message = message.substring(index + EXCEPTION_SUFIX.length()).trim();
				}
			}
		}
		
		return message;
	}
}
