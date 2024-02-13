package hiconic.rx.validation.wire.space;

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
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.registerInterceptor("validation").register(validationProcessor());
	}
	
	@Managed
	private ValidationProcessor validationProcessor() {
		ValidationProcessor bean = new ValidationProcessor();
		bean.setValidation(validation());
		return bean;
	}
	
	@Managed
	private Validation validation() {
		Validation bean = new Validation(platform.mdResolver());
		return bean;
	}

}
