// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
// ============================================================================
package hiconic.rx.platform.processing.push;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.InternalPushRequest;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.PushRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.PushResponse;
import com.braintribe.model.service.api.result.PushResponseMessage;
import com.braintribe.model.service.api.result.ResponseEnvelope;
import com.braintribe.model.service.api.result.ServiceResult;

public class PushProcessor extends AbstractDispatchingServiceProcessor<PushRequest, PushResponse> implements InitializationAware {
	private static final Logger log = Logger.getLogger(PushProcessor.class);

	private final List<ServiceProcessor<? super InternalPushRequest, PushResponse>> handlers = new CopyOnWriteArrayList<>();
	private String targetApplicationId;
	private Long requestTimeout;
	private InstanceId targetInstanceId;

	@Configurable
	public void setTargetApplicationId(String targetApplicationId) {
		this.targetApplicationId = targetApplicationId;
	}

	@Configurable
	public void setRequestTimeout(Long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public void addHandler(ServiceProcessor<? super InternalPushRequest, PushResponse> handler) {
		handlers.add(handler);
	}

	@Override
	public void postConstruct() {
		targetInstanceId = InstanceId.T.create();
		targetInstanceId.setApplicationId(targetApplicationId);
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<PushRequest, PushResponse> dispatching) {
		dispatching.register(PushRequest.T, this::push);
		dispatching.register(InternalPushRequest.T, this::internalPush);
	}

	private PushResponse internalPush(ServiceRequestContext context, InternalPushRequest request) {
		PushResponse response = PushResponse.T.create();
		for (ServiceProcessor<? super InternalPushRequest, PushResponse> handler : handlers) {
			try {
				PushResponse handlerResponse = handler.process(context, request);
				if (handlerResponse != null)
					response.getResponseMessages().addAll(handlerResponse.getResponseMessages());
			} catch (Exception e) {
				log.error("Error while executing push handler: " + handler, e);
			}
		}
		return response;
	}

	private PushResponse push(ServiceRequestContext context, PushRequest request) {
		InternalPushRequest internalRequest = cloneToInternalRequest(request);
		MulticastRequest multicast = MulticastRequest.T.create();
		multicast.setServiceRequest(internalRequest);
		multicast.setAddressee(targetInstanceId);
		if (requestTimeout != null)
			multicast.setTimeout(requestTimeout);

		MulticastResponse multicastResponse = multicast.eval(context).get();
		PushResponse response = PushResponse.T.create();

		for (Map.Entry<InstanceId, ServiceResult> entry : multicastResponse.getResponses().entrySet()) {
			ServiceResult result = entry.getValue();
			switch (result.resultType()) {
				case success -> {
					PushResponse individualResponse = (PushResponse) ((ResponseEnvelope) result).getResult();
					if (individualResponse != null && individualResponse.sentMessages())
						response.getResponseMessages().addAll(individualResponse.getResponseMessages());
				}
				case failure -> response.getResponseMessages().add(failureMessage(entry.getKey(), (Failure) result));
				default -> log.warn("Unsupported multicast push response from " + entry.getKey() + ": " + result);
			}
		}

		return response;
	}

	private PushResponseMessage failureMessage(InstanceId instanceId, Failure failure) {
		PushResponseMessage message = PushResponseMessage.T.create();
		message.setMessage(failure.getMessage());
		message.setSuccessful(false);
		message.setOriginId(instanceId);
		return message;
	}

	private InternalPushRequest cloneToInternalRequest(PushRequest request) {
		InternalPushRequest internalPush = InternalPushRequest.T.create();
		for (Property property : request.entityType().getProperties())
			internalPush.write(property, request.read(property));
		return internalPush;
	}
}
