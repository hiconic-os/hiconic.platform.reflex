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
import hiconic.rx.module.api.config.PropertyResolver;

public class RxPropertyResolver implements PropertyResolver {
	private Map<String, String> rawProperties = Collections.emptyMap();
	private final Map<String, Maybe<String>> resolvedProperties = new ConcurrentHashMap<>();
	private VirtualEnvironment virtualEnvironment = StandardEnvironment.INSTANCE;
	private boolean managedPropertiesOnly;

	@Configurable
	public void setRawProperties(Map<String, String> rawProperties) {
		this.rawProperties = rawProperties;
	}

	@Configurable
	public void setVirtualEnvironment(VirtualEnvironment virtualEnvironment) {
		this.virtualEnvironment = virtualEnvironment;
	}

	/**
	 * Disables all implicit system-property and environment fallbacks. Imported values must already be materialized in {@link #rawProperties}.
	 */
	@Configurable
	public void setManagedPropertiesOnly(boolean managedPropertiesOnly) {
		this.managedPropertiesOnly = managedPropertiesOnly;
	}

	@Override
	public String resolve(String name) {
		Maybe<String> maybe = resolveReasoned(name);
		if (maybe.isUnsatisfiedBy(PropertyNotFound.T))
			return null;

		return maybe.get();
	}

	@Override
	public Maybe<String> resolveReasoned(String name) {
		// DO NOT USE computeIfAbsent() as resolveRaw may do a recursive call back here!!!
		Maybe<String> result = resolvedProperties.get(name);
		if (result == null)
			resolvedProperties.put(name, result = resolveRaw(name));

		return result;
	}

	/**
	 * Resolves the content of a configuration placeholder. Unlike {@link #resolveReasoned(String)},
	 * the supplied value may itself be a property function such as {@code decrypt('...')}.
	 */
	public Maybe<String> resolvePlaceholderReasoned(String placeholder) {
		var functionCall = RxPropertyExpression.functionCall(placeholder);
		if (functionCall.isPresent())
			return resolveFunction(functionCall.get());

		return resolveReasoned(placeholder);
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
		String value = rawProperties.get(name);
		if (value != null)
			return value;

		if (managedPropertiesOnly)
			return null;

		if (name.startsWith(PropertyResolutions.ENV_PREFIX)) {
			String envName = name.substring(PropertyResolutions.ENV_PREFIX.length());

			return virtualEnvironment.getEnv(envName);
		}

		value = virtualEnvironment.getProperty(name);
		if (value != null)
			return value;

		return virtualEnvironment.getEnv(name);
	}

	private Maybe<String> resolveFunction(RxPropertyExpression.FunctionCall function) {
		Maybe<String> paramMaybe = function.nestedProperty()
				.map(this::resolveReasoned)
				.orElseGet(() -> Maybe.complete(function.unquotedParameter()));
		if (paramMaybe.isUnsatisfied())
			return Reasons.build(UnresolvedProperty.T)
					.text("Could not resolve parameter [" + function.rawParameter() + "] for method [" + function.name() + "]")
					.cause(paramMaybe.whyUnsatisfied())
					.toMaybe();

		return switch (function.name()) {
			case "decrypt" -> decryptReasoned(paramMaybe.get());
			default -> Reasons.build(UnsupportedOperation.T).text("Unsupported operation: " + function.name()).toMaybe();
		};
	}

	public Maybe<String> decryptReasoned(String encryptedValue) {
		Maybe<String> maybeSecret = resolveReasoned(RxPlatform.PROPERTY_DECRYPT_SECRET);
		if (maybeSecret.isUnsatisfied())
			return Reasons.build(ConfigurationError.T).text("Could not resolve decryption secret")
					.cause(maybeSecret.whyUnsatisfied()).toMaybe();

		try {
			return Maybe.complete(Cryptor.decrypt(maybeSecret.get(), null, null, null, encryptedValue));
		} catch (Exception e) {
			return Reasons.build(ConfigurationError.T).text("Wrong decryption secret").toMaybe();
		}
	}

	private class PlaceholderResolutionContext {
		private final Consumer<Reason> errorConsumer;

		public PlaceholderResolutionContext(Consumer<Reason> errorConsumer) {
			this.errorConsumer = errorConsumer;
		}

		public String resolvePlaceholder(String placeholder) {
			var maybe = RxPropertyResolver.this.resolvePlaceholderReasoned(placeholder);
			if (maybe.isSatisfied())
				return maybe.get();

			var error = UnresolvedPlaceholder.create(placeholder);
			error.getReasons().add(maybe.whyUnsatisfied());
			errorConsumer.accept(error);
			return "?";
		}

	}
}
