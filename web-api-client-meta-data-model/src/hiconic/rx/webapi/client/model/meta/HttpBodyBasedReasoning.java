package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Common representation settings for reasoning rules which decode the HTTP response body. */
@Abstract
@Description("Base metadata for HTTP reasoning rules that decode a response body.")
public interface HttpBodyBasedReasoning extends HttpReasoning {
	EntityType<HttpBodyBasedReasoning> T = EntityTypes.T(HttpBodyBasedReasoning.class);

	/** Optional marshalling MIME type. If absent, the response Content-Type and then the endpoint response MIME type are used. */
	@Description("Optional MIME type used to decode the HTTP error body.")
	String getMimeType();
	void setMimeType(String mimeType);
}
