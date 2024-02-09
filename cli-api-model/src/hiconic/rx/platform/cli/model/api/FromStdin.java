package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("An instance of this type will be evaluated to the unmarshalled content read from stdin.")
@PositionalArguments({"mimeType"})
public interface FromStdin extends From {
	EntityType<FromStdin> T = EntityTypes.T(FromStdin.class);
}
