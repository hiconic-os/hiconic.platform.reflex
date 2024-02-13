package hiconic.rx.setup.model;

import com.braintribe.devrock.templates.model.artifact.CreateArtifact;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Creates a Reflex application.")
public interface CreateReflexApp extends CreateArtifact {

	EntityType<CreateReflexApp> T = EntityTypes.T(CreateReflexApp.class);

	@Alias("m")
	@Description("Specifies whether the app is also a module, or is just an aggregator of its dependencies.")
	boolean getIsModule();
	void setIsModule(boolean isModule);

	@Override
	default String template() {
		return ReflexTemplateLocator.template("reflex-app-template");
	}

}
