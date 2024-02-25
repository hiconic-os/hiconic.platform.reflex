<#if !request.sampleProcessor>
    ${template.ignore()}
</#if>
<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.greetProcessorFull))}package ${context.processingPackage};

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import ${context.modelApiPackage}.Greet;

public class GreetProcessor implements ReasonedServiceProcessor<Greet, String> {

	@Override
	public Maybe<? extends String> processReasoned(ServiceRequestContext context, Greet request) {
		String greeting = "Hello " + request.getName();

		return Maybe.complete(greeting);
	}

}
