package hiconic.rx.access.module.processing;

import com.braintribe.model.access.IncrementalAccess;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.service.ConfiguredModel;

public record RxAccess(Access access, IncrementalAccess incrementalAccess, ConfiguredModel configuredModel) {}
