// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.platform.reflex.auth.processor;

import com.braintribe.cfg.Configurable;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.securityservice.api.exceptions.InvalidCredentialsException;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.utils.encryption.Cryptor;
import com.braintribe.utils.lcd.StringTools;

/**
 * <p>
 * This experts validates whether the provided password ({@link UserPasswordCredentials#getPassword()}) matches the
 * password known for the user given by {@link UserPasswordCredentials#getUserIdentification()}.
 * 
 */
public class UserPasswordCredentialsAuthenticationServiceProcessor extends PasswordBasedAuthenticationServiceProcessor<UserPasswordCredentials>
		implements UserIdentificationValidation {

	private static Logger log = Logger.getLogger(UserPasswordCredentialsAuthenticationServiceProcessor.class);

	private String decryptSecret = null; // TribefireRuntime.getProperty(TribefireRuntime.ENVIRONMENT_TRIBEFIRE_DECRYPT_SECRET)

	@Configurable
	public void setDecryptSecret(String decryptSecret) {
		this.decryptSecret = decryptSecret;
	}

	@Override
	protected Maybe<AuthenticateCredentialsResponse> authenticateCredentials(ServiceRequestContext context, AuthenticateCredentials request,
			UserPasswordCredentials credentials) {

		Reason reason = validateCredentialsAndDecrypt(credentials);

		if (reason != null)
			return reason.asMaybe();

		return authenticate(credentials.getUserIdentification(), credentials.getPassword()) //
				.map(this::buildAuthenticatedUserFrom);
	}

	/**
	 * <p>
	 * Validates the given credentials
	 * 
	 * @param credentials
	 *            The credentials to be validated
	 * @throws InvalidCredentialsException
	 *             If the given credentials are invalid
	 */
	private Reason validateCredentialsAndDecrypt(UserPasswordCredentials credentials) throws InvalidCredentialsException {

		Reason identificationReason = validateUserIdentification(credentials.getUserIdentification());

		if (identificationReason != null)
			return identificationReason;

		String password = credentials.getPassword();
		if (password == null) {
			log.debug(() -> "Password is null in the given credentials: [ " + credentials + " ]");
			return Reasons.build(InvalidCredentials.T).text("Invalid credentials").toReason();
		}

		if (credentials.getPasswordIsEncrypted()) {
			if (decryptSecret == null)
				return Reasons.build(InvalidCredentials.T).text("Invalid credentials")
						.cause(Reasons.build(UnsupportedOperation.T).text("Credential password decryption not supported").toReason()).toReason();

			String encryptedPassword = password;
			if (password.startsWith("${decrypt(") && password.endsWith(")}")) {
				encryptedPassword = password.substring(10, password.length() - 2);
			}
			if ((encryptedPassword.startsWith("\"") && encryptedPassword.endsWith("\""))
					|| (encryptedPassword.startsWith("'") && encryptedPassword.endsWith("'"))) {
				encryptedPassword = encryptedPassword.substring(1, encryptedPassword.length() - 1);
			}
			if (log.isDebugEnabled()) {
				log.debug("Decrypting password " + StringTools.simpleObfuscatePassword(encryptedPassword) + " with secret "
						+ StringTools.simpleObfuscatePassword(decryptSecret));
			}

			try {
				String decryptedPassword = Cryptor.decrypt(decryptSecret, null, null, null, encryptedPassword);
				credentials.setPassword(decryptedPassword);
			} catch (Exception e) {
				log.debug(() -> "Error while trying to decrypt password " + StringTools.simpleObfuscatePassword(password), e);
				return Reasons.build(InvalidCredentials.T).text("Invalid credentials").toReason();
			}
		}

		return null;
	}

}
