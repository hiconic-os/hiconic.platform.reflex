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
package hiconic.rx.platform.models;

import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;

public class RxPlatformModel extends AbstractRxConfiguredModel {

	private GmMetaModel model;
	
	public RxPlatformModel(RxConfiguredModels configuredModels, GmMetaModel model) {
		super(configuredModels);
		this.model = model;
	}

	@Override
	public String modelName() {
		return model.getName();
	}

	@Override
	protected ModelOracle buildModelOracle() {
		return new BasicModelOracle(model);
	}
}
