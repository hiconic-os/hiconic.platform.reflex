package hiconic.rx.platform.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.cfg.Required;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;

import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.platform.service.wire.RxServiceDomainWireModule;
import hiconic.rx.platform.service.wire.contract.RxServiceDomainContract;

public class RxServiceDomains implements ServiceDomains {
	private Map<String, WireContext<RxServiceDomainContract>> domains = new ConcurrentHashMap<>();
	private WireContext<?> parentWireContext;
	private ConfigurableDispatchingServiceProcessor fallbackProcessor;
	
	@Required
	public void setFallbackProcessor(ConfigurableDispatchingServiceProcessor fallbackProcessor) {
		this.fallbackProcessor = fallbackProcessor;
	}
	
	@Required
	public void setParentWireContext(WireContext<?> parentWireContext) {
		this.parentWireContext = parentWireContext;
	}
	
	@Override
	public RxServiceDomain main() {
		return byId("main");
	}

	@Override
	public RxServiceDomain byId(String domainId) {
		WireContext<RxServiceDomainContract> wireContext = domains.get(domainId);
		
		if (wireContext == null)
			return null;
		
		return wireContext.contract().serviceDomain();
	}
	
	public RxServiceDomain acquire(String domainId) {
		return domains.computeIfAbsent(domainId, this::createServiceDomain).contract().serviceDomain();
	}
	
	private WireContext<RxServiceDomainContract> createServiceDomain(String domainId) {
		WireContext<RxServiceDomainContract> wireContext = Wire.contextBuilder(new RxServiceDomainWireModule(domainId, fallbackProcessor)) //
				.parent(parentWireContext) //
				.build();
		
		return wireContext;
	}

	@Override
	public List<RxServiceDomain> list() {
		return domains.values().stream() // 
			.map(c -> c.contract().serviceDomain()) //
			.toList();
	}
}
