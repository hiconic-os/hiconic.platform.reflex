package hiconic.rx.cli.processing;

import java.util.function.Function;

import com.braintribe.cfg.Required;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

import hiconic.rx.platform.cli.model.api.Options;

public class CliEntityFactory implements Function<EntityType<?>, GenericEntity> {
	private Options options;
	
	@Required
	public void setOptions(Options options) {
		this.options = options;
	}
	
	@Override
	public GenericEntity apply(EntityType<?> t) {
		if (t == Options.T)
			return options;
		
		return t.create();
	}
}
