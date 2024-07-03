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

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.model.processing.securityservice.api.exceptions.InvalidCredentialsException;
import com.braintribe.model.securityservice.credentials.identification.EmailIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;
import com.braintribe.utils.lcd.StringTools;

public interface UserIdentificationValidation {
	default Reason validateUserIdentification(UserIdentification userIdentification) throws InvalidCredentialsException {
		return validateUserIdentification(userIdentification, null);
	}

	default Reason validateUserIdentification(UserIdentification userIdentification, String identificationDesc) throws InvalidCredentialsException {

		if (userIdentification == null)
			return Reasons.build(InvalidCredentials.T)
					.text("Missing " + (identificationDesc == null ? "" : identificationDesc + " ") + "user identification").toReason();

		if (userIdentification instanceof UserNameIdentification) {
			return validateUserNameIdentification((UserNameIdentification) userIdentification);
		} else if (userIdentification instanceof EmailIdentification) {
			return validateEmailIdentification((EmailIdentification) userIdentification);
		}

		return null;
	}

	default Reason validateUserNameIdentification(UserNameIdentification userNameIdentification) throws InvalidCredentialsException {
		if (StringTools.isBlank(userNameIdentification.getUserName())) {
			return Reasons.build(InvalidCredentials.T).text("Missing user name").toReason();
		}
		return null;
	}

	default Reason validateEmailIdentification(EmailIdentification emailIdentification) throws InvalidCredentialsException {
		if (StringTools.isBlank(emailIdentification.getEmail())) {
			return Reasons.build(InvalidCredentials.T).text("Missing e-mail").toReason();
		}

		return null;
	}

}
