package hiconic.rx.webapi.model.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Deepest cause recording the HTTP status that selected an error mapping. */
public interface HttpStatusReason extends Reason {
	EntityType<HttpStatusReason> T = EntityTypes.T(HttpStatusReason.class);

	Integer getStatusCode();
	void setStatusCode(Integer statusCode);
}
