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
package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Stable web-server filter position whose authentication intelligence may be contributed by an optional module.
 * If security is inactive, or no delegate is bound, the request passes through unchanged.
 */
public class DelegatingAuthenticationContextFilter implements Filter {

	private final BooleanSupplier securityActive;
	private volatile Filter delegate;

	public DelegatingAuthenticationContextFilter(BooleanSupplier securityActive) {
		this.securityActive = Objects.requireNonNull(securityActive, "securityActive must not be null");
	}

	public synchronized void bindDelegate(Filter delegate) {
		Objects.requireNonNull(delegate, "delegate must not be null");

		if (this.delegate != null)
			throw new IllegalStateException("An authentication context delegate is already bound: " + this.delegate.getClass().getName());

		this.delegate = delegate;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		Filter currentDelegate = delegate;

		if (!securityActive.getAsBoolean() || currentDelegate == null) {
			chain.doFilter(request, response);
			return;
		}

		currentDelegate.doFilter(request, response, chain);
	}
}
