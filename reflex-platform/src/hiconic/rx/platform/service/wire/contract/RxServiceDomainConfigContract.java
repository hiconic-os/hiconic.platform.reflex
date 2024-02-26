package hiconic.rx.platform.service.wire.contract;

import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.space.WireSpace;

public interface RxServiceDomainConfigContract extends WireSpace {
	String domainId();

	ConfigurableDispatchingServiceProcessor fallbackProcessor();
}
