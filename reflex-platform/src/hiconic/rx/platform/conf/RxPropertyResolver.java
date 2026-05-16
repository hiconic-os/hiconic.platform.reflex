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
package hiconic.rx.platform.conf;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.braintribe.cfg.Configurable;
import com.braintribe.gm.config.yaml.PropertyResolutions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonAggregator;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.config.PropertyNotFound;
import com.braintribe.gm.model.reason.config.UnresolvedPlaceholder;
import com.braintribe.gm.model.reason.config.UnresolvedProperty;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.template.Template;
import com.braintribe.utils.encryption.Cryptor;
import com.braintribe.ve.api.VirtualEnvironment;
import com.braintribe.ve.impl.StandardEnvironment;

import hiconic.rx.module.api.common.RxPlatform;

public class RxPropertyResolver {
	private Map<String, String> rawProperties = Collections.emptyMap();
	private final Map<String, Maybe<String>> resolvedProperties = new ConcurrentHashMap<>();
	private VirtualEnvironment virtualEnvironment = StandardEnvironment.INSTANCE;

	@Configurable
	public void setRawProperties(Map<String, String> rawProperties) {
		this.rawProperties = rawProperties;
	}

	@Configurable
	public void setVirtualEnvironment(VirtualEnvironment virtualEnvironment) {
		this.virtualEnvironment = virtualEnvironment;
	}

	public String resolve(String name) {
		Maybe<String> maybe = resolveReasoned(name);
		if (maybe.isUnsatisfiedBy(PropertyNotFound.T))
			return null;

		return maybe.get();
	}

	public Maybe<String> resolveReasoned(String name) {
		// DO NOT USE computeIfAbsent() as resolveRaw may do a recursive call back here!!!
		Maybe<String> result = resolvedProperties.get(name);
		if (result == null)
			resolvedProperties.put(name, result = resolveRaw(name));

		return result;
	}

	private Maybe<String> resolveRaw(String name) {
		String rawValue = findRawValue(name);

		if (rawValue == null)
			return PropertyNotFound.create(name).asMaybe();

		ReasonAggregator<UnresolvedProperty> errorAggregator = Reasons.aggregatorForceWrap(() -> UnresolvedProperty.create(name));

		String value = evaluate(rawValue, errorAggregator);

		if (errorAggregator.hasReason())
			return errorAggregator.get().asMaybe();

		return Maybe.complete(value);
	}

	private String evaluate(String rawValue, Consumer<Reason> errorConsumer) {
		Template template;
		try {
			template = Template.parse(rawValue);

		} catch (IllegalArgumentException e) {
			var error = Reasons.build(ParseError.T) //
					.text("Could not parse expression [" + rawValue + "]: " + e.getMessage()) //
					.toReason();

			errorConsumer.accept(error);
			return null;
		}

		if (template.isStaticOnly())
			return rawValue;

		var placeholderResolutionContext = new PlaceholderResolutionContext(errorConsumer);

		return template.evaluate(placeholderResolutionContext::resolvePlaceholder);
	}

	private String findRawValue(String name) {
		if (name.startsWith(PropertyResolutions.ENV_PREFIX)) {
			String envName = name.substring(PropertyResolutions.ENV_PREFIX.length());

			return virtualEnvironment.getEnv(envName);
		}

		String value = rawProperties.get(name);
		if (value != null)
			return value;

		value = virtualEnvironment.getProperty(name);
		if (value != null)
			return value;

		return virtualEnvironment.getEnv(name);
	}

	private class PlaceholderResolutionContext {
		private final Consumer<Reason> errorConsumer;

		public PlaceholderResolutionContext(Consumer<Reason> errorConsumer) {
			this.errorConsumer = errorConsumer;
		}

		public String resolvePlaceholder(String placeholder) {
			var maybe = resolvePlaceholderReasoned(placeholder);
			if (maybe.isSatisfied())
				return maybe.get();

			var error = UnresolvedPlaceholder.create(placeholder);
			error.getReasons().add(maybe.whyUnsatisfied());
			errorConsumer.accept(error);
			return "?";
		}

		private Maybe<String> resolvePlaceholderReasoned(String placeholder) {
			if (placeholder.contains("(") && placeholder.endsWith(")")) {
				int idx1 = placeholder.indexOf("(");
				int idx2 = placeholder.lastIndexOf(")");
				if (idx1 > 0 && idx2 > idx1) {
					String method = placeholder.substring(0, idx1);
					String rawParam = placeholder.substring(idx1 + 1, idx2);

					String param;
					if (rawParam.startsWith("${") && rawParam.endsWith("}")) {
						// To support param being another variable "${decrypt(${SECRET_PARAM})} 
						String nestedProperty = rawParam.substring(2, rawParam.length() - 1);
						var paramMaybe = resolveReasoned(nestedProperty);
						if (paramMaybe.isUnsatisfied())
							return Reasons.build(UnresolvedProperty.T) //
									.text("Could not resolve parameter [" + rawParam + "] for method [" + method + "]") //
									.cause(paramMaybe.whyUnsatisfied()) //
									.toMaybe();
						
						param = paramMaybe.get();

					} else {
						param = rawParam;
					}

					switch (method) {
						case "decrypt": {
							if ((param.startsWith("'") && param.endsWith("'")) || (param.startsWith("\"") && param.endsWith("\"")))
								param = param.substring(1, param.length() - 1);

							Maybe<String> maybeSecret = resolveReasoned(RxPlatform.PROPERTY_DECRYPT_SECRET);
							if (maybeSecret.isUnsatisfied())
								return Reasons.build(ConfigurationError.T).text("Could not resolve decryption secret") //
										.cause(maybeSecret.whyUnsatisfied()).toMaybe();

							try {
								String secret = maybeSecret.get();
								String decryptedValue = Cryptor.decrypt(secret, null, null, null, param);
								return Maybe.complete(decryptedValue);

							} catch (Exception e) {
								return Reasons.build(ConfigurationError.T).text("Wrong decryption secret").toMaybe();
							}
						}

						default:
							return Reasons.build(UnsupportedOperation.T).text("Unsupported operation: " + method).toMaybe();
					}
				}
			}

			return resolveReasoned(placeholder);
		}
	}
}
