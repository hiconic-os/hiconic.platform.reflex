package hiconic.rx.module.api.common;

import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

/**
 * @author peter.gazdik
 */
public interface RxPlatform {
	static String PROPERTY_DECRYPT_SECRET = "RX_DECRYPT_SECRET";

	// TODO is there a better place for User-related info than RxPlatform? 
	
	static UserSession currentUserSession() {
		return AttributeContexts.peek().findOrNull(UserSessionAspect.class);
	}

	static String currentUserSessionId() {
		UserSession userSession = currentUserSession();
		return userSession == null ? null : userSession.getSessionId();
	}

	static String currentUserName() {
		UserSession userSession = currentUserSession();
		if (userSession == null)
			return null;

		User user = userSession.getUser();
		if (user == null)
			return null;

		return user.getName();
	}

}
