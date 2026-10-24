package hiconic.rx.webapi.model.reason;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** HTTP status evidence enriched with a successfully decoded structured response body. */
public interface HttpStatusWithEntityBody extends HttpStatusWithBody {
	EntityType<HttpStatusWithEntityBody> T = EntityTypes.T(HttpStatusWithEntityBody.class);

	GenericEntity getBody();
	void setBody(GenericEntity body);
}
