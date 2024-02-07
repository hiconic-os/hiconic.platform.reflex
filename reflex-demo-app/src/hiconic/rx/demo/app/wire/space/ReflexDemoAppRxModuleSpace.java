package hiconic.rx.demo.app.wire.space;

import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._ReflexDemoModel_;
import hiconic.rx.demo.app.processing.DataGenerationSource;
import hiconic.rx.demo.app.processing.PersonRequestProcessor;
import hiconic.rx.demo.app.processing.ReverseTextProcessor;
import hiconic.rx.demo.model.api.PersonRequest;
import hiconic.rx.demo.model.api.ReverseText;
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
		dispatching.register(PersonRequest.T, personRequestProcessor());
	}
	
	@Managed
	private ReverseTextProcessor reverseTextProcessor() {
		return new ReverseTextProcessor();
	}
	
	@Managed
	private PersonRequestProcessor personRequestProcessor() {
		PersonRequestProcessor bean = new PersonRequestProcessor();
		bean.setDataGenerationSourceSupplier(this::dataGenerationSource);
		return bean;
	}
	
	@Managed
	private DataGenerationSource dataGenerationSource() {
		return new DataGenerationSource();
	}
}
