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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.config.PropertyNotFound;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.template.Template;
import com.braintribe.model.generic.template.TemplateFragment;

import hiconic.rx.module.api.common.RxPlatform;
import hiconic.rx.platform.conf.RxPropertyExpression;

/**
 * Resolves the closed part of a layered property graph without consulting the build machine's environment.
 * <p>
 * A property which ultimately depends on an external leaf remains symbolic. Consumers can use {@link #externalLeaves(Collection)} to translate
 * intermediate local property names back to the actual deployment imports.
 */
public final class SymbolicConfigurationProperties {

	private final Map<String, String> rawProperties;
	private final Map<String, Resolution> resolutions;

	private SymbolicConfigurationProperties(Map<String, String> rawProperties, Map<String, Resolution> resolutions) {
		this.rawProperties = Map.copyOf(rawProperties);
		this.resolutions = Map.copyOf(resolutions);
	}

	public static Maybe<SymbolicConfigurationProperties> analyze(Map<String, String> rawProperties) {
		Map<String, String> stableRawProperties = new LinkedHashMap<>(rawProperties);
		Map<String, Resolution> resolutions = new LinkedHashMap<>();
		var errors = Reasons.aggregatorForceWrap(() -> ConfigurationError.create("Invalid configuration property graph"));

		for (String name : stableRawProperties.keySet()) {
			Resolution resolution = resolve(name, stableRawProperties, resolutions, new ArrayDeque<>());
			if (resolution.error() != null)
				errors.accept(resolution.error());
		}

		SymbolicConfigurationProperties result = new SymbolicConfigurationProperties(stableRawProperties, resolutions);
		if (errors.hasReason())
			return Maybe.incomplete(result, errors.get());

		return Maybe.complete(result);
	}

	/** Returns a value only when the complete local property expression is closed. */
	public Maybe<String> resolveKnown(String name) {
		Resolution resolution = resolutions.get(name);
		if (resolution == null || !resolution.closed())
			return PropertyNotFound.create(name).asMaybe();

		if (resolution.error() != null)
			return resolution.error().asMaybe();

		return Maybe.complete(resolution.value());
	}

	/** Replaces local symbolic aliases with the undeclared external leaves on which they depend. */
	public Set<String> externalLeaves(Collection<String> unresolvedNames) {
		Set<String> result = new LinkedHashSet<>();
		for (String name : unresolvedNames) {
			Resolution resolution = resolutions.get(name);
			if (resolution == null)
				result.add(name);
			else
				result.addAll(resolution.externalLeaves());
		}
		return result;
	}

	public Map<String, String> rawProperties() {
		return rawProperties;
	}

	public Map<String, String> resolvedProperties() {
		Map<String, String> result = new LinkedHashMap<>();
		resolutions.forEach((name, resolution) -> {
			if (resolution.error() == null && resolution.closed())
				result.put(name, resolution.value());
		});
		return result;
	}

	private static Resolution resolve(String name, Map<String, String> rawProperties, Map<String, Resolution> resolutions, Deque<String> stack) {
		Resolution known = resolutions.get(name);
		if (known != null)
			return known;

		if (stack.contains(name)) {
			String cycle = String.join(" -> ", stack) + " -> " + name;
			Resolution result = Resolution.error(ConfigurationError.create("Cyclic configuration property reference: " + cycle));
			resolutions.put(name, result);
			return result;
		}

		String rawValue = rawProperties.get(name);
		if (rawValue == null)
			return Resolution.external(name);

		Template template;
		try {
			template = Template.parse(rawValue);
		} catch (IllegalArgumentException e) {
			Resolution result = Resolution.error(Reasons.build(ParseError.T)
					.text("Could not parse configuration property [" + name + "]: " + e.getMessage())
					.toReason());
			resolutions.put(name, result);
			return result;
		}

		stack.addLast(name);
		StringBuilder value = new StringBuilder();
		Set<String> externalLeaves = new LinkedHashSet<>();
		com.braintribe.gm.model.reason.Reason error = null;
		boolean closed = true;

		for (TemplateFragment fragment : template.fragments()) {
			if (!fragment.isPlaceholder()) {
				value.append(fragment.getText());
				continue;
			}

			Resolution nested = resolveExpression(fragment.getText(), rawProperties, resolutions, stack);
			if (nested.error() != null) {
				error = nested.error();
				break;
			}

			if (nested.closed())
				value.append(nested.value());
			else {
				closed = false;
				externalLeaves.addAll(nested.externalLeaves());
			}
		}

		stack.removeLast();
		Resolution result = error == null
				? new Resolution(closed ? value.toString() : null, Set.copyOf(externalLeaves), null, closed)
				: Resolution.error(error);
		resolutions.put(name, result);
		return result;
	}

	private static Resolution resolveExpression(String expression, Map<String, String> rawProperties,
			Map<String, Resolution> resolutions, Deque<String> stack) {
		var functionCall = RxPropertyExpression.functionCall(expression);
		if (functionCall.isEmpty())
			return resolve(expression, rawProperties, resolutions, stack);

		RxPropertyExpression.FunctionCall function = functionCall.get();
		if (!function.name().equals("decrypt"))
			return Resolution.error(Reasons.build(UnsupportedOperation.T)
					.text("Unsupported configuration property operation: " + function.name())
					.toReason());

		Set<String> externalLeaves = new LinkedHashSet<>();
		Resolution secret = resolve(RxPlatform.PROPERTY_DECRYPT_SECRET, rawProperties, resolutions, stack);
		if (secret.error() != null)
			return secret;
		externalLeaves.addAll(secret.externalLeaves());

		if (function.nestedProperty().isPresent()) {
			Resolution parameter = resolve(function.nestedProperty().get(), rawProperties, resolutions, stack);
			if (parameter.error() != null)
				return parameter;
			externalLeaves.addAll(parameter.externalLeaves());
		}

		// Even a fully closed decrypt expression deliberately remains symbolic at build time.
		return new Resolution(null, Set.copyOf(externalLeaves), null, false);
	}

	private record Resolution(String value, Set<String> externalLeaves, com.braintribe.gm.model.reason.Reason error, boolean closed) {
		private static Resolution external(String name) {
			return new Resolution(null, Set.of(name), null, false);
		}

		private static Resolution error(com.braintribe.gm.model.reason.Reason error) {
			return new Resolution(null, Set.of(), error, false);
		}
	}
}
