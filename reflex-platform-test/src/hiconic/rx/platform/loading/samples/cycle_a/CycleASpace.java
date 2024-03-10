package hiconic.rx.platform.loading.samples.cycle_a;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.samples.api.ApiSpace;
import hiconic.rx.platform.loading.samples.api.CycleContract;

/**
 * @author peter.gazdik
 */
@Managed
public class CycleASpace implements RxModuleContract {

	@Import
	private ApiSpace api;

	@Import
	private CycleContract cycle;

}
