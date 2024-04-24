package hiconic.rx.access.module.api;

import com.braintribe.model.generic.reflection.EntityType;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.wire.RxExportContract;

public interface AccessExpertContract extends RxExportContract {

	<A extends Access> void registerAccessExpert(EntityType<A> accessType, AccessExpert<A> expert);
}
