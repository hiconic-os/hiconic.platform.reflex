package hiconic.rx.platform.loading.samples.exporter;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.samples.api.ApiSpace;

/**
 * @author peter.gazdik
 */
@Managed
public class ExporterSpace implements RxModuleContract {

	@Import
	private ApiSpace exportedSpace;

}
