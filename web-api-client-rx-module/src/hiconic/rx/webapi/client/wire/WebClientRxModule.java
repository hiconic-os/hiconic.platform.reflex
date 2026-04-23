package hiconic.rx.webapi.client.wire;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.webapi.client.api.WebApiClientContract;
import hiconic.rx.webapi.client.wire.space.WebClientRxModuleSpace;

public enum WebClientRxModule implements RxModule<WebClientRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(WebApiClientContract.class, WebClientRxModuleSpace.class);
	}

}