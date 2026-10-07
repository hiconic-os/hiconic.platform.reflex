package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Decodes the HTTP response body as the authoritative top-level reason. */
@Description("Decodes the HTTP response body as the top-level Reason.")
public interface HttpBodyReasoning extends HttpRootReasoning, HttpBodyBasedReasoning {
	EntityType<HttpBodyReasoning> T = EntityTypes.T(HttpBodyReasoning.class);
	String getReasonTypeSignature();
	void setReasonTypeSignature(String reasonTypeSignature);
}
