package hiconic.rx.platform.loading;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.samples.api.ApiContract;
import hiconic.rx.platform.loading.samples.api.ApiSpace;
import hiconic.rx.platform.loading.samples.exporter.ExporterSpace;
import hiconic.rx.platform.loading.samples.exporter.ExportingModule;

/**
 * @author peter.gazdik
 */
public class RxModuleLoadingTest {

	@Test
	public void canLoadModule() throws Exception {
		WireContext<?> wireContext = Wire.contextBuilder(ExportingModule.INSTANCE) //
				.bindContract(ApiContract.class, ApiSpace.class) //
				.build();

		RxModuleContract exporterSpace = (RxModuleContract) wireContext.contract();
		assertThat(exporterSpace).isNotNull();
		assertThat(exporterSpace.getClass().getName()).isEqualTo(ExporterSpace.class.getName());
	}

}
