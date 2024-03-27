package hiconic.platform.reflex.web_rpc_client.wire.space;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.webrpc.client.GmWebRpcRemoteServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.rpc.client.api.WebRpcClientContract;
import hiconic.rx.web.rpc.client.model.config.WebRpcClientConnection;
import hiconic.rx.web.rpc.client.model.config.WebRpcRemoteServiceDomain;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WebRpcClientRxModuleSpace implements RxModuleContract, WebRpcClientContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public Evaluator<ServiceRequest> remoteEvaluator(WebRpcClientConnection connection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void installRemoteDomain(WebRpcRemoteServiceDomain connection) {
		// TODO Auto-generated method stub
		
	}
	
	private GmWebRpcRemoteServiceProcessor remoteServiceProcessor(WebRpcClientConnection connection) {
		GmWebRpcRemoteServiceProcessor bean = new GmWebRpcRemoteServiceProcessor();
		bean.setUrl(connection.getUrl());
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		return bean;
	}

}