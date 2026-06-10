package hiconic.rx.platform.processing.cluster;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.ServiceResults;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ServiceResult;

public class SingleInstanceMulticastProcessor implements ReasonedServiceProcessor<MulticastRequest, MulticastResponse> {
	private InstanceId instanceId;

	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}
	
	@Override
	public Maybe<? extends MulticastResponse> processReasoned(ServiceRequestContext context, MulticastRequest request) {
		InstanceId addressee = request.getAddressee();
		
		// test if app is either wildcard or a match
		if (!(addressee.getApplicationId() == null || addressee.getApplicationId().equals(instanceId.getApplicationId())))
			return Maybe.complete(MulticastResponse.T.create());
		
		// test if node is either wildcard or a match
		if (!(addressee.getNodeId() == null || addressee.getNodeId().equals(instanceId.getNodeId())))
			return Maybe.complete(MulticastResponse.T.create());
		
		ServiceRequest actualRequest = request.getServiceRequest();
		
		Maybe<?> maybe = actualRequest.eval(context).getReasoned();
		
		ServiceResult serviceResult = ServiceResults.fromMaybe(maybe);
		
		MulticastResponse response = MulticastResponse.T.create();
		response.getResponses().put(instanceId, serviceResult);

		return Maybe.complete(response);
	}
}
