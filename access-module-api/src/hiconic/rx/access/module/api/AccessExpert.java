package hiconic.rx.access.module.api;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.access.IncrementalAccess;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.service.ConfiguredModel;

public interface AccessExpert<A extends Access> {
	Maybe<IncrementalAccess> deploy(A access, ConfiguredModel dataModel);
}
