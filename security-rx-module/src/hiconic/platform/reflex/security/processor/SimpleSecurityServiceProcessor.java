package hiconic.platform.reflex.security.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.securityservice.SimplifiedOpenUserSession;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;

/**
 * The service processor that handles all the simplified open user session requests.
 *
 */
public class SimpleSecurityServiceProcessor extends AbstractDispatchingServiceProcessor<SimplifiedOpenUserSession, OpenUserSessionResponse> {

	@Override
	protected void configureDispatching(DispatchConfiguration<SimplifiedOpenUserSession, OpenUserSessionResponse> dispatching) {
		dispatching.registerReasoned(OpenUserSessionWithUserAndPassword.T, this::openUserSessionWithUserAndPassword);
	}

	private Maybe<? extends OpenUserSessionResponse> openUserSessionWithUserAndPassword(ServiceRequestContext requestContext,
			OpenUserSessionWithUserAndPassword request) {
		OpenUserSession openUserSession = OpenUserSession.T.create();

		openUserSession.setLocale(request.getLocale());
		openUserSession.setMetaData(request.getMetaData());

		UserPasswordCredentials credentials = UserPasswordCredentials.T.create();
		openUserSession.setCredentials(credentials);
		credentials.setPassword(request.getPassword());

		UserNameIdentification userIdentification = UserNameIdentification.T.create();
		credentials.setUserIdentification(userIdentification);
		userIdentification.setUserName(request.getUser());

		return openUserSession.eval(requestContext).getReasoned();
	}

}
