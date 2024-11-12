package hiconic.platform.reflex.security.processor;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.User;

import hiconic.platform.reflex.security.api.UserInternalService;

public class JdbcUserService implements UserInternalService {

	@Override
	public User findUser(String propertyName, String propertyValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification) throws UserNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User retrieveUser(String propertyName, String propertyValue) throws UserNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String retrieveUserId(UserIdentification userIdentification) throws UserNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String retrieveUserName(String propertyName, String propertyValue) throws UserNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reason ensureUser(User user) {
		// TODO Auto-generated method stub
		return null;
	}

}
