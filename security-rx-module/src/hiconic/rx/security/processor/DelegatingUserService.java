package hiconic.rx.security.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.User;

import hiconic.rx.security.api.UserService;

/**
 * @author peter.gazdik
 */
public class DelegatingUserService implements UserService {

	private String userServiceId;

	private final Map<String, UserService> userServices = new HashMap<>();

	private volatile UserService delegate;

	public synchronized void registerUserService(UserService userService) {
		userServices.put(userService.userServiceId(), userService);
	}

	public void setDelegateUserServiceId(String userServiceId) {
		this.userServiceId = userServiceId;
	}

	@Override
	public String userServiceId() {
		return userServiceId;
	}

	@Override
	public User findUser(String propertyName, String propertyValue) {
		return delegate().findUser(propertyName, propertyValue);
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification) throws UserNotFoundException {
		return delegate().retrieveUser(userIdentification);
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException {
		return delegate().retrieveUser(userIdentification, password);
	}

	@Override
	public User retrieveUser(String propertyName, String propertyValue) throws UserNotFoundException {
		return delegate().retrieveUser(propertyName, propertyValue);
	}

	@Override
	public String retrieveUserId(UserIdentification userIdentification) throws UserNotFoundException {
		return delegate().retrieveUserId(userIdentification);
	}

	@Override
	public String retrieveUserName(String propertyName, String propertyValue) throws UserNotFoundException {
		return delegate().retrieveUserName(propertyName, propertyValue);
	}

	@Override
	public Reason ensureUser(User user) {
		return delegate().ensureUser(user);
	}

	@Override
	public Reason reconcileUsers(List<User> users, String provisioningGroup, String reconciliationRevision) {
		return delegate().reconcileUsers(users, provisioningGroup, reconciliationRevision);
	}

	@Override
	public Reason reconcileUsers(List<User> users, String provisioningGroup, String reconciliationRevision, boolean deleteVanishedUsers) {
		return delegate().reconcileUsers(users, provisioningGroup, reconciliationRevision, deleteVanishedUsers);
	}

	private UserService delegate() {
		if (delegate != null)
			return delegate;

		synchronized (this) {
			delegate = userServices.get(userServiceId);
			if (delegate == null)
				throw new IllegalStateException("No UserService registered (yet) with id " + userServiceId);

			return delegate;
		}
	}

}
