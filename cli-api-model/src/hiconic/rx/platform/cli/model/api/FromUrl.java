package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("An instance of this type will be evaluated to the unmarshalled content read from the configured url.")
@PositionalArguments({"url", "mimeType"})
public interface FromUrl extends From {
	EntityType<FromUrl> T = EntityTypes.T(FromUrl.class);

	@Description("The url from which the content should be read")
	@Alias("u")
	@Mandatory
	String getUrl();
	void setUrl(String file);
}
