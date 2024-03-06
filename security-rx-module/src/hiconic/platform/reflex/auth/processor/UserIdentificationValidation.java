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
