package hiconic.rx.webapi.model.reason;

import com.braintribe.gm.model.reason.essential.CommunicationError;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface HttpReason extends CommunicationError {

	EntityType<HttpReason> T = EntityTypes.T(HttpReason.class);

	Integer getHttpCode();
	void setHttpCode(Integer httpCode);

	String getHttpPayload();
	void setHttpPayload(String httpPayload);

	@Override
	default String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(CommunicationError.super.asString());
		sb.append(" HTTP code: ").append(getHttpCode());
		sb.append(" HTTP payload: ").append(getHttpPayload());
		return sb.toString();
	}

}
