package hiconic.rx.access.module.api;

import java.util.function.Supplier;

import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.accessrequest.api.AccessRequestProcessor;

import hiconic.rx.module.api.service.ModelConfiguration;

public interface AccessServiceModelConfiguration extends ModelConfiguration {
	<R extends AccessRequest> void bindAccessRequest(EntityType<R> request, Supplier<AccessRequestProcessor<? super R, ?>> processorSupplier);
}
