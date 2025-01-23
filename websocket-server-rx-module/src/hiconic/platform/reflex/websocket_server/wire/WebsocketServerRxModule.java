package hiconic.platform.reflex.websocket_server.wire;

import hiconic.platform.reflex.websocket_server.wire.space.WebsocketServerRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.push.api.PushContract;

public enum WebsocketServerRxModule implements RxModule<WebsocketServerRxModuleSpace> {

	INSTANCE;
	
	@Override
	public void bindExports(Exports exports) {
		exports.bind(PushContract.class, WebsocketServerRxModuleSpace.class);
	}
}