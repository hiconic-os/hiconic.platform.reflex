// ============================================================================
package hiconic.rx.platform.test.wire.space;

import com.braintribe.gm._BasicResourceModel_;
import com.braintribe.model.processing.service.api.ServiceAroundProcessor;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._ResourceStorageApiModel_;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.test.PlatformTestDomains;
import hiconic.rx.platform.test.wire.contract.PlatformTestContract;

@Managed
public class PlatformRxTestModuleSpace implements RxModuleContract, PlatformTestContract {
	public static final String resourcesAlias = "legacy-resources";

	@Import
	private RxPlatformContract platform;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration sd = configurations.byId(PlatformTestDomains.resources);
		sd.addAlias(resourcesAlias);
		sd.addModel(_ResourceStorageApiModel_.reflection);
		sd.addModel(_BasicResourceModel_.reflection);
		sd.bindInterceptor("first-test-interceptor").forType(GetResourcePayload.T).bind(this::forwardingInterceptor);
		sd.bindInterceptor("second-test-interceptor").forType(GetResourcePayload.T).bind(this::forwardingInterceptor);
		sd.orderInterceptors("first-test-interceptor", "second-test-interceptor");

		// A module-provided multicast processor must unambiguously override the platform's single-instance fallback.
		configurations.internal().bindRequest(MulticastRequest.T, () -> (context, request) -> MulticastResponse.T.create());
	}

	private ServiceAroundProcessor<GetResourcePayload, Object> forwardingInterceptor() {
		return (context, request, proceed) -> proceed.proceed(request);
	}

}
