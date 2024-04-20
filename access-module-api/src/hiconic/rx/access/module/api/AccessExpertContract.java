package hiconic.rx.access.module.api;

import java.util.function.Supplier;

import com.braintribe.model.generic.reflection.EntityType;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.wire.RxExportContract;

public interface AccessExpertContract extends RxExportContract {

	// 1. there must be an expert for a given access type that can handle persistence requests in the end
	void registerAccessExpert(EntityType<? extends Access> accessType, Supplier<? extends AccessExpert> expertSupplier);
}
