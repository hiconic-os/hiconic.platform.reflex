// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2026
//
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
package hiconic.rx.platform.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.model.generic.GenericEntity;

/**
 * Result of statically assembling the modeled part of an application's configuration.
 * <p>
 * This deliberately is a plain processing value rather than another persisted model. The report is modeled because it crosses the build/runtime
 * boundary; the assembled entities already carry their own reflected types.
 */
public record ConfigurationAssembly(
		Map<ConfigurationKey, GenericEntity> configurations,
		Map<String, String> rawProperties,
		Map<String, String> resolvedProperties,
		Set<String> consumedResources,
		ConfigurationAssemblyReport report) {

	public ConfigurationAssembly {
		configurations = Map.copyOf(configurations);
		rawProperties = Map.copyOf(rawProperties);
		resolvedProperties = Map.copyOf(resolvedProperties);
		consumedResources = Set.copyOf(consumedResources);
	}

	public List<ConfigurationKey> keys() {
		return configurations.keySet().stream().sorted().toList();
	}
}
