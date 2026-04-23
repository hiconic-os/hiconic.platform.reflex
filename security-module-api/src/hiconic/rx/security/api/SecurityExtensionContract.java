package hiconic.rx.security.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.GrantedCredentials;
import com.braintribe.model.securityservice.credentials.TrustedCredentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;

import hiconic.rx.module.api.wire.RxExportContract;

public interface SecurityExtensionContract extends RxExportContract {

	/**
	 * Registers a processor for handling {@link AuthenticateCredentials}.
	 * <p>
	 * The purpose of this processor is to provide a {@link User} instance (or even a {@link UserSession}) with properly set roles and groups.
	 * <p>
	 * The platform comes with processors for the following credentials:
	 * <ul>
	 * <li>{@link ExistingSessionCredentials}
	 * <li>{@link GrantedCredentials}
	 * <li>{@link UserPasswordCredentials}
	 * <li>{@link TrustedCredentials}
	 * </ul>
	 */
	<C extends Credentials> void registerCredentialProcessor(EntityType<C> credentialType,
			ServiceProcessor<? extends AuthenticateCredentials, AuthenticateCredentialsResponse> processor);

}
