package hiconic.rx.check.api;

import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;

/**
 * @author peter.gazdik
 */
public interface CheckProcessor {

	/**
	 * Performs one or more checks and reports the results for each as a separate {@link CheckResult#getEntries() entry},
	 * 
	 * @see CheckResult 
	 * @see CheckResultEntry 
	 */
	CheckResult check(ServiceRequestContext context);

}
