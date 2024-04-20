package hiconic.rx.hello.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.hello.model.api.Greet;
import hiconic.rx.hello.processing.GreetProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class ReflexHelloAppRxModuleSpace implements RxModuleContract {
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(Greet.T, this::greetProcessor);
	}

	@Managed
	private GreetProcessor greetProcessor() {
		return new GreetProcessor();
	}
}
