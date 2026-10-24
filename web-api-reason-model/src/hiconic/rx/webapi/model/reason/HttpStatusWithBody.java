package hiconic.rx.webapi.model.reason;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** HTTP status evidence enriched with the received response-body metadata. */
public interface HttpStatusWithBody extends HttpStatusReason {
	EntityType<HttpStatusWithBody> T = EntityTypes.T(HttpStatusWithBody.class);

	String getMimeType();
	void setMimeType(String mimeType);

	Long getBodySize();
	void setBodySize(Long bodySize);

	Boolean getBodyTruncated();
	void setBodyTruncated(Boolean bodyTruncated);
}
