package hiconic.platform.reflex.security.processor;

import java.util.concurrent.Callable;

import com.braintribe.cfg.Required;
import com.braintribe.model.processing.securityservice.api.UserSessionScope;
import com.braintribe.model.processing.securityservice.api.UserSessionScoping;
import com.braintribe.model.processing.worker.api.Worker;
import com.braintribe.model.processing.worker.api.WorkerAspect;
import com.braintribe.model.processing.worker.api.WorkerAspectContext;
import com.braintribe.model.processing.worker.api.WorkerContext;

/**
 * {@link WorkerAspect} that ensures the {@link Worker}'s {@link WorkerContext#submit(Callable) job} is authorized as the system user.
 * 
 * @author peter.gazdik
 */
public class SystemUserScopingWorkerAspect implements WorkerAspect {

	private UserSessionScoping userSessionScoping;

	@Required
	public void setUserSessionScoping(UserSessionScoping userSessionScoping) {
		this.userSessionScoping = userSessionScoping;
	}

	@Override
	public Object run(WorkerAspectContext context) throws Exception {
		UserSessionScope scope = userSessionScoping.forDefaultUser().push();
		try {
			return context.proceed();

		} finally {
			scope.pop();
		}
	}

}
