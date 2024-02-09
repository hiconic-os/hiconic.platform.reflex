package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.FileResource;

@Description("An instance of this type will be evaluated to the unmarshalled content read from the configured file.")
@PositionalArguments({"file", "mimeType"})
public interface FromFile extends From {
	EntityType<FromFile> T = EntityTypes.T(FromFile.class);

	@Description("The file from which the content should be read")
	@Alias("f")
	@Mandatory
	FileResource getFile();
	void setFile(FileResource file);
}
