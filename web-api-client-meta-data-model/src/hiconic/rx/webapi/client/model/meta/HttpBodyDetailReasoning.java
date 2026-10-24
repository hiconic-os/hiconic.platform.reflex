package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Decodes a structured HTTP error body as evidence carried by the selected HTTP status reason. */
@Description("Decodes a structured HTTP error body as evidence on the HTTP status reason.")
public interface HttpBodyDetailReasoning extends HttpBodyBasedReasoning {
	EntityType<HttpBodyDetailReasoning> T = EntityTypes.T(HttpBodyDetailReasoning.class);
	/** Legacy property name retained for compatibility; identifies the entity expected in the response body. */
	String getReasonTypeSignature();
	void setReasonTypeSignature(String reasonTypeSignature);
}
