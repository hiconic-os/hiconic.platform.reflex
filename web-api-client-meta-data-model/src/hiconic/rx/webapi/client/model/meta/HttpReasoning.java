package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/**
 * Base metadata for interpreting an HTTP response as an unsatisfied service result. Root and body-detail rules are resolved independently; exactly
 * one root creates the result reason and the response body is consumed at most once.
 */
@Abstract
@Description("Interprets matching HTTP responses as unsatisfied service results.")
public interface HttpReasoning extends EntityTypeMetaData {
	EntityType<HttpReasoning> T = EntityTypes.T(HttpReasoning.class);
	@Description("HTTP status selector supporting exact codes, families, ranges, unions and exclusions.")
	String getStatusCodeExpression();
	void setStatusCodeExpression(String statusCodeExpression);
}
