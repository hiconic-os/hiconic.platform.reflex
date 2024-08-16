package hiconic.rx.access.module.api;

import java.util.Set;

public interface AccessDomains {
	Set<String> domainIds();
	boolean hasDomain(String domainId);
	AccessDomain byId(String domainId);
}
