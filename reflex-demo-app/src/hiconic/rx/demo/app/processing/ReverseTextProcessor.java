package hiconic.rx.demo.app.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.demo.model.ReverseText;

public class ReverseTextProcessor implements ReasonedServiceProcessor<ReverseText, String> {
	@Override
	public Maybe<? extends String> processReasoned(ServiceRequestContext context, ReverseText request) {
		String text = request.getText();
		
		if ("forbidden".equals(text))
			return Reasons.build(InvalidArgument.T).text("ReverseText.text must not be 'forbidden'").toMaybe();
		
		if (text == null)
			return Maybe.complete(null);
		
		String result = new StringBuilder(text).reverse().toString();
		
		return Maybe.complete(result);
	}
}
