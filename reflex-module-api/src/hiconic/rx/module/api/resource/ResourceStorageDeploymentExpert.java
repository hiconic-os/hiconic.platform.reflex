// ============================================================================
package hiconic.rx.module.api.resource;

import com.braintribe.gm.model.reason.Maybe;

import hiconic.rx.module.api.wire.RxPlatformConfigurator;

/**
 * Expert for instantiating a {@link ResourceStorage} implementation out of a {@link hiconic.rx.resource.model.configuration.ResourceStorage}
 * denotation type.
 * <p>
 * It is used when resource storage is configured in a YAML file, where the denotation instance is specified.
 * <p>
 * These experts are registered via {@link RxPlatformConfigurator#registerResourceStorageDeploymentExpert}.
 */
@FunctionalInterface
public interface ResourceStorageDeploymentExpert<RS extends hiconic.rx.resource.model.configuration.ResourceStorage> {

	Maybe<ResourceStorage> deploy(RS storageDenotation);

}
