package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Help extends CliRequest {
	EntityType<Help> T = EntityTypes.T(Help.class);
}
