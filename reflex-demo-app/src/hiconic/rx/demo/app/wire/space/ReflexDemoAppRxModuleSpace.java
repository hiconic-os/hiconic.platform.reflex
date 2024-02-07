package hiconic.rx.demo.app.wire.space;

import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._ReflexDemoModel_;
import hiconic.rx.demo.app.processing.ReverseTextProcessor;
import hiconic.rx.demo.model.ReverseText;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class ReflexDemoAppRxModuleSpace implements RxModuleContract {
	@Override
	public void addApiModels(ConfigurationModelBuilder builder) {
		builder.addDependency(_ReflexDemoModel_.reflection);
	}
	
	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.register(ReverseText.T, reverseTextProcessor());
	}
	
	@Managed
	private ReverseTextProcessor reverseTextProcessor() {
		ReverseTextProcessor bean = new ReverseTextProcessor();
		return bean;
	}
}
