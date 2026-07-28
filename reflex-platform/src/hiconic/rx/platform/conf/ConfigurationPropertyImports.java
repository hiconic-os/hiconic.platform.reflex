// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.conf;

import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.gm.config.assembly.model.ConfigurationImport;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.ve.api.VirtualEnvironment;

import hiconic.rx.platform.configuration.ConfigurationImportDeclarations;

/**
 * Materializes explicitly declared deployment imports into the managed RX property graph.
 * <p>
 * Environment and system properties are deliberately consulted only here. Downstream configuration resolution therefore has one complete,
 * inspectable source map rather than an implicit fallback into the host process.
 */
public final class ConfigurationPropertyImports {

	private ConfigurationPropertyImports() {
	}

	public static Maybe<Map<String, String>> bind(Map<String, String> rawProperties,
			ConfigurationImportDeclarations declarations, VirtualEnvironment environment) {
		Map<String, String> result = new LinkedHashMap<>(rawProperties);
		var errors = Reasons.aggregatorForceWrap(() -> ConfigurationError.create("Missing required configuration imports"));

		for (ConfigurationImport declaration : declarations.imports()) {
			String name = declaration.getName();
			if (result.containsKey(name))
				continue;

			String value = environment.getProperty(name);
			if (value == null)
				value = environment.getEnv(name);

			if (value != null) {
				result.put(name, value);
				continue;
			}

			if (declaration.getRequired())
				errors.accept(ConfigurationError.create("Required configuration import [" + name + "] was not supplied"));
		}

		if (errors.hasReason())
			return errors.get().asMaybe();

		return Maybe.complete(Map.copyOf(result));
	}
}
