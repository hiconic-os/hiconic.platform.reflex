package hiconic.platform.reflex.explorer.processing.bapi;

import com.braintribe.model.bapi.CurrentUserInformationRequest;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessorException;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;

/**
 * @author peter.gazdik
 */
public class CurrentUserInformationProcessor implements ServiceProcessor<CurrentUserInformationRequest, User> {

	@Override
	public User process(ServiceRequestContext requestContext, CurrentUserInformationRequest request) throws ServiceProcessorException {
		return requestContext.findAttribute(UserSessionAspect.class) //
				.map(UserSession::getUser) //
				.orElse(null);
	}

}
