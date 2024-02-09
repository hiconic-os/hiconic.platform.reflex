package hiconic.rx.hello.wire.space;

import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._HelloWorldModel_;
import hiconic.rx.hello.model.api.Greet;
import hiconic.rx.hello.processing.GreetProcessor;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class ReflexHelloAppRxModuleSpace implements RxModuleContract {
	@Override
	public void addApiModels(ConfigurationModelBuilder builder) {
		builder.addDependency(_HelloWorldModel_.reflection);
	}
	
	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.register(Greet.T, greetProcessor());
	}

	@Managed
	private GreetProcessor greetProcessor() {
		return new GreetProcessor();
	}
}
