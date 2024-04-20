package hiconic.rx.web.rpc.client.api;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ModelReference;
import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.web.rpc.client.model.config.WebRpcClientConnection;

public interface WebRpcClientContract extends RxExportContract {

	Evaluator<ServiceRequest> remoteEvaluator(WebRpcClientConnection connection);
	ModelReference remoteServiceConfigurationModel(WebRpcClientConnection connection);
	ServiceProcessor<ServiceRequest, Object> remoteServiceProcessor(WebRpcClientConnection connection);
}
