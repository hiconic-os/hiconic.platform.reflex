package hiconic.platform.reflex.websocket_server.wire.space;

import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.PushRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.platform.reflex.websocket_server.processing.PushChannelLifecycleHub;
import hiconic.platform.reflex.websocket_server.processing.WsServer;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.push.api.PushContract;
import hiconic.rx.web.server.api.WebServerContract;

@Managed
public class WebsocketServerRxModuleSpace implements RxModuleContract, PushContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private WebServerContract webServer;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		webServer.addEndpoint("/ws", server());
		webServer.addEndpoint("/websocket", server());
	}
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		// TODO: use PushRequest, MulticastRequest and InternalPushRequest to enable PushRequest in distributed setups
		configuration.bindRequest(PushRequest.T, this::server);
	}
	
	@Managed
	private WsServer server() {
		WsServer bean = new WsServer();
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setProcessingInstanceId(InstanceId.of(platform.nodeId(), "reflex"));
		bean.setEvaluator(platform.systemEvaluator());
		bean.setPushChannelLifecycleHub(channelLifecyclePublisher());
		return bean;
	}
	
	@Managed
	@Override
	public PushChannelLifecycleHub channelLifecyclePublisher() {
		PushChannelLifecycleHub bean = new PushChannelLifecycleHub();
		return bean;
	}
}