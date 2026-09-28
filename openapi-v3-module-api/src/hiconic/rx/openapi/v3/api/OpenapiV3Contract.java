package hiconic.rx.openapi.v3.api;

import hiconic.rx.module.api.wire.RxExportContract;
import tribefire.extension.webapi.openapi_v3.api.OpenapiDescriptionResolverRegistry;

public interface OpenapiV3Contract extends RxExportContract {
	OpenapiDescriptionResolverRegistry descriptionResolverRegistry();
}
