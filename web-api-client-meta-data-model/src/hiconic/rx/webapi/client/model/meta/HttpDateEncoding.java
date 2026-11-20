package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Date encoding local to a textual HTTP parameter; does not configure body marshalling. */
@Abstract
public interface HttpDateEncoding extends GenericEntity {

	EntityType<HttpDateEncoding> T = EntityTypes.T(HttpDateEncoding.class);

	@Description("Optional DateTimeFormatter pattern for this parameter. If absent, legacy date formatting applies.")
	String getDateFormat();
	void setDateFormat(String dateFormat);

	@Description("Zone used to convert a Date to local fields. Requires dateFormat; defaults to the JVM zone if absent.")
	String getDateZone();
	void setDateZone(String dateZone);
}
