package hiconic.rx.platform.loading.samples.exporter;

import com.braintribe.wire.api.context.WireContextBuilder;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.platform.loading.samples.api.ApiContract;
import hiconic.rx.platform.loading.samples.api.ApiSpace;

/**
 * @author peter.gazdik
 */
public enum ExportingModule implements RxModule<ExporterSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(ApiContract.class, ApiSpace.class);
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		RxModule.super.configureContext(contextBuilder);

		contextBuilder.bindContract(ApiContract.class, ApiSpace.class);
	}

}
