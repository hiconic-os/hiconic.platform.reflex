package hiconic.rx.openapi.v3.api;

import java.util.function.Consumer;

import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.PropertyMdResolver;

public interface OpenapiDescriptionResolver {
	default void resolveEntityDescription(ModelMdResolver modelMdResolver, EntityMdResolver entityMdResolver, Consumer<String> consumer) {}
	default void resolvePropertyDescription(ModelMdResolver modelMdResolver, PropertyMdResolver mdResolver, Consumer<String> consumer) {}
}
