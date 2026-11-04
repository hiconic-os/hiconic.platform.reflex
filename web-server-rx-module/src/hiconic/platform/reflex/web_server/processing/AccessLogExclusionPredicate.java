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
package hiconic.platform.reflex.web_server.processing;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;

/** Keeps failed requests visible while suppressing configured successful-path noise. */
public class AccessLogExclusionPredicate implements io.undertow.predicate.Predicate {

	private final Set<Predicate<String>> excludes;

	public AccessLogExclusionPredicate(Set<String> excludes) {
		this.excludes = excludes == null ? Set.of() : excludes.stream()
				.filter(pattern -> pattern != null && !pattern.isBlank())
				.map(AccessLogExclusionPredicate::glob)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	@Override
	public boolean resolve(HttpServerExchange exchange) {
		AccessLogOutcome.complete(exchange);
		if (AccessLogFailureTrackingHandler.failed(exchange) || exchange.getAttachment(ExceptionHandler.THROWABLE) != null
				|| !exchange.isResponseStarted() || exchange.getStatusCode() >= 400)
			return true;
		String path = exchange.getRequestPath();
		return excludes.stream().noneMatch(exclude -> exclude.test(path));
	}

	static Predicate<String> glob(String glob) {
		StringBuilder regex = new StringBuilder("^");
		for (int i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);
			if (c == '*') {
				if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
					regex.append(".*");
					i++;
				} else {
					regex.append("[^/]*");
				}
			} else if (c == '?') {
				regex.append("[^/]");
			} else {
				regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(regex.append('$').toString()).asMatchPredicate();
	}
}
