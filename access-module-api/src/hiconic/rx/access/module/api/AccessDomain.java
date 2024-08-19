package hiconic.rx.access.module.api;

import com.braintribe.model.access.IncrementalAccess;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.service.ConfiguredModel;

public interface AccessDomain {
	Access access();
	IncrementalAccess incrementalAccess();
	ConfiguredModel configuredDataModel();
	ConfiguredModel configuredServiceModel();
}
