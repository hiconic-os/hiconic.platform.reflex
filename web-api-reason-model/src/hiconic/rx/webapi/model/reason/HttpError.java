package hiconic.rx.webapi.model.reason;

import com.braintribe.gm.model.reason.essential.CommunicationError;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Generic logical error used when an HTTP status has no more specific Reason mapping. */
public interface HttpError extends CommunicationError {
	EntityType<HttpError> T = EntityTypes.T(HttpError.class);
}
