package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Decodes a structured HTTP error body as a detail cause beneath the selected root reason. */
@Description("Decodes a structured HTTP error body as a detail cause.")
public interface HttpBodyDetailReasoning extends HttpBodyBasedReasoning {
	EntityType<HttpBodyDetailReasoning> T = EntityTypes.T(HttpBodyDetailReasoning.class);
	String getReasonTypeSignature();
	void setReasonTypeSignature(String reasonTypeSignature);
}
