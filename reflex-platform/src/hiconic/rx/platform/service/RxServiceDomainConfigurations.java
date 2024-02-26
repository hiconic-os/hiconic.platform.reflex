package hiconic.rx.platform.service;

import java.util.List;

import com.braintribe.cfg.Required;

import hiconic.rx.module.api.service.ServiceDomainConfigurations;

public class RxServiceDomainConfigurations implements ServiceDomainConfigurations {
	private RxServiceDomains serviceDomains;

	@Required
	public void setServiceDomains(RxServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}
	
	@Override
	public RxServiceDomain byId(String domainId) {
		return serviceDomains.acquire(domainId);
	}

	@Override
	public RxServiceDomain main() {
		return byId("main");
	}

	public List<RxServiceDomain> list() {
		return serviceDomains.list();
	}
}
