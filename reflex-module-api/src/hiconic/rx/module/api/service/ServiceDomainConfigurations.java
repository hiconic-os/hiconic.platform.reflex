package hiconic.rx.module.api.service;

public interface ServiceDomainConfigurations {
	ServiceDomainConfiguration byId(String domainId);
	ServiceDomainConfiguration main();
}
