package hiconic.platform.reflex.web_rpc.wire.space;

import com.braintribe.model.processing.webrpc.server.GmWebRpcServer;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WebRpcServerRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Managed
	private GmWebRpcServer webRpcServer() {
		GmWebRpcServer bean = new GmWebRpcServer();
		
		bean.setEvaluator(platform.evaluator());
		bean.setMarshallerRegistry(platform.marshallers());
		
		return bean;
	}
}