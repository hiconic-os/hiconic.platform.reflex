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
package hiconic.rx.security.audit;

import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.credentials.AbstractUserIdentificationCredentials;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.TokenWithUserNameCredentials;
import com.braintribe.model.securityservice.credentials.identification.EmailIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;

/** Security-relevant authentication diagnostics without credential contents. */
public final class SecurityAudit {

	private static final Logger log = Logger.getLogger(SecurityAudit.class);

	private SecurityAudit() {
	}

	public static void authenticationFailed(String reason, UserIdentification identification, ServiceRequestContext context) {
		logFailure(reason, identity(identification), context, null);
	}

	public static void sessionOpeningFailed(String reason, Credentials credentials, ServiceRequestContext context, String entryPoint) {
		logFailure(reason, identity(credentials), context, entryPoint);
	}

	private static void logFailure(String reason, String identity, ServiceRequestContext context, String entryPoint) {
		String requestorAddress = context == null ? null : context.getRequestorAddress();
		log.info("Security authentication failure [reason=" + safe(reason) + ", identity=" + safe(identity) + ", requestor="
				+ safe(requestorAddress) + ", entryPoint=" + safe(entryPoint) + "]");
	}

	private static String identity(Credentials credentials) {
		if (credentials instanceof AbstractUserIdentificationCredentials identified)
			return identity(identified.getUserIdentification());
		if (credentials instanceof TokenWithUserNameCredentials token)
			return "user:" + token.getUserName();
		return credentials == null ? null : "credentials-type:" + credentials.entityType().getTypeSignature();
	}

	private static String identity(UserIdentification identification) {
		if (identification instanceof UserNameIdentification userName)
			return "user:" + userName.getUserName();
		if (identification instanceof EmailIdentification email)
			return "email:" + email.getEmail();
		return identification == null ? null : "identification-type:" + identification.entityType().getTypeSignature();
	}

	private static String safe(String value) {
		return value == null ? "<none>" : value.replace('\r', '_').replace('\n', '_');
	}
}
