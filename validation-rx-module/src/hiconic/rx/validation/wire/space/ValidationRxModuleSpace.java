package hiconic.rx.validation.wire.space;

import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.cli.processing.Validation;
import hiconic.rx.platform.cli.processing.ValidationProcessor;

@Managed
public class ValidationRxModuleSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;
	
	@Override
	public void registerCrossDomainInterceptors(InterceptorRegistry interceptorBinding) {
		interceptorBinding.registerInterceptor("validation").register(validationProcessor());
	}
	
	@Managed
	private ValidationProcessor validationProcessor() {
		// TODO: validation needs to keep each service domain separate (currently everything works only on main domain)
		ValidationProcessor bean = new ValidationProcessor();
		bean.setValidation(validation());
		return bean;
	}
	
	@Managed
	private Validation validation() {
		Validation bean = new Validation(platform.serviceDomains().main().cmdResolver());
		return bean;
	}

}
