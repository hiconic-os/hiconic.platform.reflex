package hiconic.rx.platform.service.wire;

import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.platform.service.wire.contract.RxServiceDomainConfigContract;
import hiconic.rx.platform.service.wire.contract.RxServiceDomainContract;

public class RxServiceDomainWireModule implements WireTerminalModule<RxServiceDomainContract>, RxServiceDomainConfigContract {
	private String domainId;
	private ConfigurableDispatchingServiceProcessor fallbackProcessor;

	public RxServiceDomainWireModule(String domainId, ConfigurableDispatchingServiceProcessor fallbackProcessor) {
		super();
		this.domainId = domainId;
		this.fallbackProcessor = fallbackProcessor;
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxServiceDomainConfigContract.class, this);
	}
	
	@Override
	public String domainId() {
		return domainId;
	}
	
	@Override
	public ConfigurableDispatchingServiceProcessor fallbackProcessor() {
		return fallbackProcessor;
	}
	
}
