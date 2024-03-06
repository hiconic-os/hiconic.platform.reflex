package hiconic.platform.reflex.security.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.core.expert.api.MutableDenotationMap;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.credentials.Credentials;

public class AuthenticationProcessor implements ReasonedServiceProcessor<AuthenticateCredentials, AuthenticateCredentialsResponse> {
	private MutableDenotationMap<Credentials, ServiceProcessor<AuthenticateCredentials, AuthenticateCredentialsResponse>> credentialProcessors = new PolymorphicDenotationMap<>(true);
	
	public <C extends Credentials> void register(EntityType<C> credentialType, ServiceProcessor<? extends AuthenticateCredentials, AuthenticateCredentialsResponse> processor) {
		credentialProcessors.put(credentialType, (ServiceProcessor<AuthenticateCredentials, AuthenticateCredentialsResponse>) processor);
	}
	
	@Override
	public Maybe<? extends AuthenticateCredentialsResponse> processReasoned(ServiceRequestContext context,
			AuthenticateCredentials request) {
		Credentials credentials = request.getCredentials();
		
		if (credentials == null)
			return Reasons.build(InvalidArgument.T).text("AuthenticateCredentials.credentials must not be null").toMaybe();
		
		ServiceProcessor<AuthenticateCredentials, AuthenticateCredentialsResponse> delegateProcessor = credentialProcessors.find(credentials);
		
		if (delegateProcessor == null)
			return Reasons.build(UnsupportedOperation.T).text("Missing processor for credential type: " + credentials.entityType().getTypeSignature()).toMaybe();

		return Maybe.complete(delegateProcessor.process(context, request));
	}
}
