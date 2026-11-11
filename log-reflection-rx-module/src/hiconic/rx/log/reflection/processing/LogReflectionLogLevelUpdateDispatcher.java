package hiconic.rx.log.reflection.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.logging.Logger;
import com.braintribe.logging.level.LogLevelManager;
import com.braintribe.logging.level.LogLevelUpdateDispatcher;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ServiceResult;
import com.braintribe.model.service.api.result.Unsatisfied;

import hiconic.rx.log.reflection.model.api.ApplyEffectiveLogLevels;

/** Distributes a persisted level change through RX without depending on the CX log-level service API. */
public class LogReflectionLogLevelUpdateDispatcher implements LogLevelUpdateDispatcher {
	private static final Logger logger = Logger.getLogger(LogReflectionLogLevelUpdateDispatcher.class);

	private Evaluator<ServiceRequest> evaluator;
	private InstanceId localInstanceId;
	private LogLevelManager logLevelManager;
	private Long timeoutInMs;

	@Override
	public Maybe<Void> dispatchUpdate() {
		try {
			Objects.requireNonNull(logLevelManager, "logLevelManager");

			ApplyEffectiveLogLevels update = ApplyEffectiveLogLevels.T.create();
			MulticastRequest multicast = MulticastRequest.T.create();
			multicast.setAsynchronous(false);
			multicast.setServiceRequest(update);
			multicast.setAddressee(InstanceId.of(null, localInstanceId.getApplicationId()));
			if (timeoutInMs != null)
				multicast.setTimeout(timeoutInMs);

			return validate(multicast.eval(evaluator).get());
		} catch (Exception e) {
			return InternalError.createTraceback(InternalError.from(e, "Failed to multicast effective log level update"),
					"Failed to multicast effective log level update", logger::error).asMaybe();
		}
	}

	private Maybe<Void> validate(MulticastResponse response) {
		StringBuilder errors = new StringBuilder();
		List<Reason> reasons = new ArrayList<>();
		for (Map.Entry<InstanceId, ServiceResult> entry : response.getResponses().entrySet()) {
			ServiceResult result = entry.getValue();
			if (result instanceof Failure failure) {
				String message = entry.getKey() + ": " + failure.getType() + ": " + failure.getDetails();
				errors.append("\n- ").append(message);
				reasons.add(InternalError.create(message));
			} else if (result instanceof Unsatisfied unsatisfied) {
				Reason reason = unsatisfied.getWhy();
				String message = entry.getKey() + ": " + (reason == null ? "unsatisfied" : reason.asString());
				errors.append("\n- ").append(message);
				reasons.add(reason == null ? InternalError.create(message) : reason);
			}
		}

		if (errors.length() == 0)
			return Maybe.complete(null);

		InternalError reason = InternalError.create("Failed to update effective log levels on at least one cluster instance:" + errors);
		reason.getReasons().addAll(reasons);
		return Maybe.empty(InternalError.createTraceback(reason, "Failed to update effective log levels", logger::error));
	}

	@Configurable
	@Required
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
	}

	@Configurable
	@Required
	public void setLocalInstanceId(InstanceId localInstanceId) {
		this.localInstanceId = Objects.requireNonNull(localInstanceId, "localInstanceId");
	}

	@Configurable
	public void setTimeoutInMs(Long timeoutInMs) {
		this.timeoutInMs = timeoutInMs;
	}

	@Override
	@Configurable
	@Required
	public void setLogLevelManager(LogLevelManager logLevelManager) {
		this.logLevelManager = Objects.requireNonNull(logLevelManager, "logLevelManager");
	}
}
