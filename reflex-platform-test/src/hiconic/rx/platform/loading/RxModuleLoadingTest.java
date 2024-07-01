// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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
