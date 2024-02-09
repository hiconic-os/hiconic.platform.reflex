package hiconic.rx.hello.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.hello.model.api.Greet;

public class GreetProcessor implements ReasonedServiceProcessor<Greet, String> {
	@Override
	public Maybe<? extends String> processReasoned(ServiceRequestContext context, Greet request) {
		String name = request.getName();
		
		if (name == null)
			name = "nobody";

		String greeting = "Hello " + name + "!";
		
		return Maybe.complete(greeting);
	}
}
