package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Introduce extends CliRequest {
	EntityType<Introduce> T = EntityTypes.T(Introduce.class);
}
