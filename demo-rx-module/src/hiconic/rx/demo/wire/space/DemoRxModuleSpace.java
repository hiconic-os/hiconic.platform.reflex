package hiconic.rx.demo.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._DemoModel_;
import hiconic.rx.demo.model.api.PersonRequest;
import hiconic.rx.demo.model.api.ReverseText;
import hiconic.rx.demo.processing.DataGenerationSource;
import hiconic.rx.demo.processing.PersonRequestProcessor;
import hiconic.rx.demo.processing.ReverseTextProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class DemoRxModuleSpace implements RxModuleContract {
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.addModel(_DemoModel_.reflection);
		
		configuration.register(ReverseText.T, reverseTextProcessor());
		configuration.register(PersonRequest.T, personRequestProcessor());
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
