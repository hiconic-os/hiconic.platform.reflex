package hiconic.rx.platform.loading.samples.cycle_b;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.samples.api.ApiContract;
import hiconic.rx.platform.loading.samples.api.CycleSpace;

/**
 * @author peter.gazdik
 */
@Managed
public class CycleBSpace implements RxModuleContract {

	@Import
	private ApiContract api;

	@Import
	private CycleSpace cycle;

}
