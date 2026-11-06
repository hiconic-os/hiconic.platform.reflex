package hiconic.rx.browser.acceptance.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.browser.acceptance.model.BrowserAcceptance;

/**
 * Explicitly requests approval for the current browser context.
 *
 * <p>Credentials are required for ordinary callers. A trusted server-side authentication transport may instead
 * provide its already authenticated result through the request attribute context.</p>
 */
public interface RequestBrowserAcceptance extends ServiceRequest {
	EntityType<RequestBrowserAcceptance> T = EntityTypes.T(RequestBrowserAcceptance.class);

	Credentials getCredentials();
	void setCredentials(Credentials credentials);

	String getEntryPoint();
	void setEntryPoint(String entryPoint);

	@Override
	EvalContext<BrowserAcceptance> eval(Evaluator<ServiceRequest> evaluator);
}
