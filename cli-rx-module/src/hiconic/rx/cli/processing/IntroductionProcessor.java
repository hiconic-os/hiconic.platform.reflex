package hiconic.rx.cli.processing;

import com.braintribe.cfg.Required;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.result.Neutral;

import hiconic.rx.platform.cli.model.api.Introduce;

public class IntroductionProcessor implements ServiceProcessor<Introduce, Neutral>{
	private String applicationName;
	
	@Required
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	
	@Override
	public Neutral process(ServiceRequestContext requestContext, Introduce request) {
		ConsoleOutputs.println(applicationName);
		
		return Neutral.NEUTRAL;
	}
}
