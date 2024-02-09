package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface From extends GenericEntity {
	EntityType<From> T = EntityTypes.T(From.class);

	@Description("The mimetype defining the serialization format for the file input. "
			+ "Possible values: text/yaml, application/x-yaml, text/xml, application/json, gm/bin, gm/jse, gm/man")
	@Alias("m")
	@Initializer("'application/yaml'")
	@Mandatory
	String getMimeType();
	void setMimeType(String mimeType);
	
	@Description("Entities are created without initialized property default values to exactly reproduce a specific entity.")
	@Alias("r")
	boolean getReproduce();
	void setReproduce(boolean reproduce);
}
