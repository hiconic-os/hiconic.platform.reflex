package hiconic.rx.module.api.common;

import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

/**
 * @author peter.gazdik
 */
public interface RxPlatform {

	static UserSession currentUserSession() {
		return AttributeContexts.peek().findOrNull(UserSessionAspect.class);
	}

	static String currentUserSessionId() {
		UserSession userSession = currentUserSession();
		return userSession == null ? null : userSession.getSessionId();
	}

}
