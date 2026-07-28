// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.conf;

import java.util.Optional;

/**
 * Shared syntax recognition for RX property-expression functions.
 * <p>
 * Evaluation deliberately remains with the respective runtime or symbolic resolver.
 */
public final class RxPropertyExpression {

	private RxPropertyExpression() {
	}

	public static Optional<FunctionCall> functionCall(String placeholder) {
		if (!placeholder.contains("(") || !placeholder.endsWith(")"))
			return Optional.empty();

		int opening = placeholder.indexOf('(');
		int closing = placeholder.lastIndexOf(')');
		if (opening <= 0 || closing <= opening)
			return Optional.empty();

		return Optional.of(new FunctionCall(
				placeholder.substring(0, opening),
				placeholder.substring(opening + 1, closing)));
	}

	public record FunctionCall(String name, String rawParameter) {

		public Optional<String> nestedProperty() {
			if (rawParameter.startsWith("${") && rawParameter.endsWith("}"))
				return Optional.of(rawParameter.substring(2, rawParameter.length() - 1));
			return Optional.empty();
		}

		public String unquotedParameter() {
			if (rawParameter.length() >= 2
					&& (rawParameter.startsWith("'") && rawParameter.endsWith("'")
							|| rawParameter.startsWith("\"") && rawParameter.endsWith("\"")))
				return rawParameter.substring(1, rawParameter.length() - 1);
			return rawParameter;
		}
	}
}
