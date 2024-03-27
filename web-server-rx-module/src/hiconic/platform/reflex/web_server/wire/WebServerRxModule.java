package hiconic.platform.reflex.web_server.wire;

import hiconic.platform.reflex.web_server.wire.space.WebServerRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.web.server.api.WebServerContract;

public enum WebServerRxModule implements RxModule<WebServerRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(WebServerContract.class, WebServerRxModuleSpace.class);
	}
}