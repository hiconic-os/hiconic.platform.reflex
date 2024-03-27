package hiconic.rx.web.rpc.client.api;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.web.rpc.client.model.config.WebRpcClientConnection;
import hiconic.rx.web.rpc.client.model.config.WebRpcRemoteServiceDomain;

public interface WebRpcClientContract extends RxExportContract {

	Evaluator<ServiceRequest> remoteEvaluator(WebRpcClientConnection connection);
	void installRemoteDomain(WebRpcRemoteServiceDomain connection);
	
	
}
