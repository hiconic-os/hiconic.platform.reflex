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
package hiconic.rx.access.module.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import hiconic.rx.access.module.api.AccessModelConfiguration;
import hiconic.rx.access.module.api.AccessModelConfigurations;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ModelReference;

public class RxAccessModelConfigurations implements AccessModelConfigurations {
	private ModelConfigurations modelConfigurations;
	private Map<ModelConfiguration, RxAccessModelConfiguration> accessModelConfigurations = new ConcurrentHashMap<>();
	
	public void initModelConfigurations(ModelConfigurations modelConfigurations) {
		this.modelConfigurations = modelConfigurations;
	}
	
	@Override
	public AccessModelConfiguration byConfiguration(ModelConfiguration modelConfiguration) {
		return accessModelConfigurations.computeIfAbsent(modelConfiguration, RxAccessModelConfiguration::new);
	}
	
	@Override
	public AccessModelConfiguration byName(String modeName) {
		return byConfiguration(modelConfigurations.byName(modeName));
	}
	
	@Override
	public AccessModelConfiguration byReference(ModelReference modelReference) {
		return byConfiguration(modelConfigurations.byReference(modelReference));
	}
	
	@Override
	public List<AccessModelConfiguration> listConfigurations() {
		return List.copyOf(accessModelConfigurations.values());
	}
}
