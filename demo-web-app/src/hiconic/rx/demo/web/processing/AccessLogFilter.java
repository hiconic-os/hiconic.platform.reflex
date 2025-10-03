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
package hiconic.rx.demo.web.processing;

import java.io.IOException;

import com.braintribe.logging.Logger;

import dev.hiconic.servlet.api.HttpFilter;
import hiconic.rx.web.server.api.FilterSymbol;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AccessLogFilter implements HttpFilter {

	public static final FilterSymbol symbol = () -> "accessLogFilter";

	private final Logger logger = Logger.getLogger(AccessLogFilter.class);

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		long start = System.nanoTime();
		
		chain.doFilter(request, response);
		long end = System.nanoTime();
		
		long delta = end - start;
		
		logger.info("Logged " + delta + " ns");
	}
}
