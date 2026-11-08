package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.level.LogLevelManager;
import com.braintribe.logging.level.LogLevelFramework;
import com.braintribe.logging.level.persistence.LogLevelPersistence;
import com.braintribe.model.generic.eval.AbstractEvalContext;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.EvalException;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ResponseEnvelope;
import com.braintribe.processing.async.api.AsyncCallback;

import hiconic.rx.log.reflection.model.api.ApplyEffectiveLogLevels;

public class LogReflectionLogLevelUpdateDispatcherTest {
	@Test
	public void updatesEveryNodeOfTheLocalApplication() {
		AtomicReference<MulticastRequest> dispatched = new AtomicReference<>();
		MulticastResponse response = MulticastResponse.T.create();
		response.getResponses().put(InstanceId.of("node-1", "logs-app"), success());
		response.getResponses().put(InstanceId.of("node-2", "logs-app"), success());

		LogReflectionLogLevelUpdateDispatcher dispatcher = dispatcher(evaluator(request -> {
			dispatched.set((MulticastRequest) request);
			return response;
		}));

		Maybe<Void> result = dispatcher.dispatchUpdate();

		assertThat(result.isSatisfied()).isTrue();
		assertThat(dispatched.get().getAddressee().stringify()).isEqualTo("logs-app@<undefined>");
		assertThat(dispatched.get().getServiceRequest()).isInstanceOf(ApplyEffectiveLogLevels.class);
		assertThat(dispatched.get().getAsynchronous()).isFalse();
	}

	@Test
	public void rejectsPartialClusterUpdates() {
		MulticastResponse response = MulticastResponse.T.create();
		response.getResponses().put(InstanceId.of("node-1", "logs-app"), success());
		Failure failure = Failure.T.create();
		failure.setType("Timeout");
		failure.setMessage("node did not answer");
		response.getResponses().put(InstanceId.of("node-2", "logs-app"), failure);

		Maybe<Void> result = dispatcher(evaluator(ignored -> response)).dispatchUpdate();

		assertThat(result.isSatisfied()).isFalse();
		assertThat(result.whyUnsatisfied().getText()).contains("Failed to update effective log levels");
	}

	private static LogReflectionLogLevelUpdateDispatcher dispatcher(Evaluator<ServiceRequest> evaluator) {
		LogReflectionLogLevelUpdateDispatcher dispatcher = new LogReflectionLogLevelUpdateDispatcher();
		dispatcher.setEvaluator(evaluator);
		dispatcher.setLocalInstanceId(InstanceId.of("node-1", "logs-app"));
		// The dispatcher only verifies that it is attached to its owning manager; it does not call it.
		LogLevelPersistence persistence = proxy(LogLevelPersistence.class);
		dispatcher.setLogLevelManager(new LogLevelManager(persistence, persistence, proxy(LogLevelFramework.class)));
		return dispatcher;
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> null);
	}

	private static ResponseEnvelope success() {
		ResponseEnvelope envelope = ResponseEnvelope.T.create();
		envelope.setResult(null);
		return envelope;
	}

	private static Evaluator<ServiceRequest> evaluator(Function<ServiceRequest, Object> responder) {
		return new Evaluator<>() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> EvalContext<T> eval(ServiceRequest request) {
				return new AbstractEvalContext<>() {
					@Override
					public T get() throws EvalException {
						return (T) responder.apply(request);
					}

					@Override
					public void get(AsyncCallback<? super T> callback) {
						try {
							callback.onSuccess(get());
						} catch (Throwable e) {
							callback.onFailure(e);
						}
					}
				};
			}
		};
	}
}
