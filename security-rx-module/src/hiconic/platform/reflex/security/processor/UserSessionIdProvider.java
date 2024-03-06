// ============================================================================
package hiconic.platform.reflex.security.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.utils.DateTools;

public class UserSessionIdProvider implements Supplier<String>, Function<UserSessionType, String> {

	private Map<UserSessionType, String> typePrefixes = new HashMap<>();

	@Configurable
	public void setTypePrefixes(Map<UserSessionType, String> typePrefixes) {
		this.typePrefixes = typePrefixes;
	}

	@Override
	public String get() {
		//Not using the RandomTools.newStandardUuid() method with the embedded date/time as this 
		//would cryptographically weaken the ID
		//We still want to keep the date/time in the session Id to easier identify problems with expired sessions
		final String datePrefix = DateTools.encode(new Date(), DateTools.TERSE_DATETIME_WITH_MS_FORMAT);
		String uuid = UUID.randomUUID().toString();
		return datePrefix+"-"+uuid;
	}

	@Override
	public String apply(UserSessionType type) {

		String sessionId = get();

		String prefix = typePrefixes.get(type);

		if (prefix != null) {
			sessionId = prefix + sessionId;
		}

		return sessionId;
	}

}
