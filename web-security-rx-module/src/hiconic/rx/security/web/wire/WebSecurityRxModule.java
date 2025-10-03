package hiconic.rx.security.web.wire;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.security.web.api.WebSecurityContract;
import hiconic.rx.security.web.wire.space.WebSecurityRxModuleSpace;

public enum WebSecurityRxModule implements RxModule<WebSecurityRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(WebSecurityContract.class, WebSecurityRxModuleSpace.class);
	}

}