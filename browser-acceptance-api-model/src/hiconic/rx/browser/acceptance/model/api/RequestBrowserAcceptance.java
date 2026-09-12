package hiconic.rx.browser.acceptance.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.browser.acceptance.model.BrowserAcceptance;

/** Explicitly requests approval for the current browser context after authenticating the supplied credentials. */
public interface RequestBrowserAcceptance extends ServiceRequest {
	EntityType<RequestBrowserAcceptance> T = EntityTypes.T(RequestBrowserAcceptance.class);

	@Mandatory
	Credentials getCredentials();
	void setCredentials(Credentials credentials);

	String getEntryPoint();
	void setEntryPoint(String entryPoint);

	@Override
	EvalContext<BrowserAcceptance> eval(Evaluator<ServiceRequest> evaluator);
}
