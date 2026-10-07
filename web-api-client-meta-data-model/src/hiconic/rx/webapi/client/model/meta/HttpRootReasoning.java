package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Creates the top-level reason for an HTTP failure. */
@Abstract
@Description("Creates the top-level reason for a matching HTTP failure.")
public interface HttpRootReasoning extends HttpReasoning {
	EntityType<HttpRootReasoning> T = EntityTypes.T(HttpRootReasoning.class);
	boolean getUseOriginalStatusCode();
	void setUseOriginalStatusCode(boolean useOriginalStatusCode);
}
