package hiconic.rx.setup.model;

import com.braintribe.devrock.templates.model.artifact.CreateArtifact;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Creates a Reflex module.")
public interface CreateReflexModule extends CreateArtifact {

	EntityType<CreateReflexModule> T = EntityTypes.T(CreateReflexModule.class);

	@Override
	default String template() {
		return ReflexTemplateLocator.template("reflex-module-template");
	}

}
