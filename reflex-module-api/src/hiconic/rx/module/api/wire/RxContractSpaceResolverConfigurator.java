package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.space.ContractSpaceResolver;

/**
 * Configures {@link ContractSpaceResolver}s on the platform's {@link WireContext}, which is also the parent context of individual module's contexts.
 */
public interface RxContractSpaceResolverConfigurator {

	void addResolver(ContractSpaceResolver resolver);

}
