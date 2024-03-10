package hiconic.rx.platform.loading.samples.cycle_a;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.platform.loading.samples.api.ApiContract;
import hiconic.rx.platform.loading.samples.api.ApiSpace;

/**
 * @author peter.gazdik
 */
public enum CycleAModule implements RxModule<CycleASpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(ApiContract.class, ApiSpace.class);
	}
}
