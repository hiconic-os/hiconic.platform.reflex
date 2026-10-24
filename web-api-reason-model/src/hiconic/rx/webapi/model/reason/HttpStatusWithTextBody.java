package hiconic.rx.webapi.model.reason;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * HTTP status evidence whose body is retained as text. Nested reasons explain why an expected structured body could
 * not be decoded; they do not cause the remote HTTP status itself.
 */
public interface HttpStatusWithTextBody extends HttpStatusWithBody {
	EntityType<HttpStatusWithTextBody> T = EntityTypes.T(HttpStatusWithTextBody.class);

	String getBody();
	void setBody(String body);

	String getExpectedBodyTypeSignature();
	void setExpectedBodyTypeSignature(String expectedBodyTypeSignature);
}
