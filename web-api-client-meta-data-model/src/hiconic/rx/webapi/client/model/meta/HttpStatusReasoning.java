package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Maps a matching HTTP status to a logical root reason; an unclaimed body becomes its text. */
@Description("Maps matching HTTP statuses to a configured logical reason type.")
public interface HttpStatusReasoning extends HttpRootReasoning {
	EntityType<HttpStatusReasoning> T = EntityTypes.T(HttpStatusReasoning.class);
	String getReasonTypeSignature();
	void setReasonTypeSignature(String reasonTypeSignature);
}
