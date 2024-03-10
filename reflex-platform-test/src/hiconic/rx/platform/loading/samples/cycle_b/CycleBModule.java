package hiconic.rx.platform.loading.samples.cycle_b;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.platform.loading.samples.api.CycleContract;
import hiconic.rx.platform.loading.samples.api.CycleSpace;

/**
 * @author peter.gazdik
 */
public enum CycleBModule implements RxModule<CycleBSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(CycleContract.class, CycleSpace.class);
	}
}
