package hiconic.rx.platform.loading.samples.importer;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.samples.api.ApiContract;

/**
 * @author peter.gazdik
 */
@Managed
public class ImporterSpace implements RxModuleContract {

	@Import
	private ApiContract simple;

}
