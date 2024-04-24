package hiconic.rx.access.module.api;

import hiconic.rx.module.api.service.ModelConfiguration;

public interface AccessModelConfiguration extends ModelConfiguration {
	AccessInterceptorBuilder bindAspect(String identifier);
}
