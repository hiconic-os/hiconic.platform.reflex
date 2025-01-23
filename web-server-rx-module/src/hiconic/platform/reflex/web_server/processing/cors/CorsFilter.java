// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
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
package hiconic.platform.reflex.web_server.processing.cors;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;

import hiconic.platform.reflex.web_server.processing.cors.exception.CorsException;
import hiconic.platform.reflex.web_server.processing.cors.handler.CorsHandler;
import hiconic.platform.reflex.web_server.processing.cors.handler.CorsRequestType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link Filter} for handling W3C's CORS (Cross-Origin Resource Sharing) policies.
 * 
 */
public class CorsFilter implements Filter {
	
	private CorsHandler handler;

	private static final Logger log = Logger.getLogger(CorsFilter.class);
	
	@Required
	@Configurable
	public void setCorsHandler(CorsHandler handler) {
		this.handler = handler;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		if (log.isTraceEnabled()) {
			log.trace("initializing "+this.getClass().getName());
		}
	}
	
	@Override
	public void destroy() {
		if (log.isTraceEnabled()) {
			log.trace("destroying "+this.getClass().getName());
		}
	}
	
	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		if (log.isTraceEnabled()) {
			logHeaders(request);
		}
		
		CorsRequestType requestType = CorsRequestType.get(request);
		
		if (requestType.equals(CorsRequestType.nonCors)) {
			
			if (log.isTraceEnabled()) {
				log.trace("Non-CORS request");
			}
			
			chain.doFilter(request, response);
			
		} else {
			
			if (log.isTraceEnabled()) {
				log.trace("CORS request: "+requestType);
			}
			
			try {
				if (requestType.equals(CorsRequestType.preflight)) {
					
					handler.handlePreflight(request, response);
					
				} else if (requestType.equals(CorsRequestType.actual)) {
					
					handler.handleActual(request, response);
					
					chain.doFilter(request, response);
					
				}
				
			} catch (CorsException cex) {
				
				response.setStatus(cex.getHttpResponseCode());
				response.resetBuffer();
				response.setContentType("text/plain");
				PrintWriter out = response.getWriter();
				out.println("Failed to apply Cross-Origin Resource Sharing (CORS) policy: " + cex.getMessage());
				
			}
		}
		
		if (log.isTraceEnabled()) {
			logHeaders(request, response);
		}
		
	}

	private static void logHeaders(HttpServletRequest request) {

		if (!log.isTraceEnabled()) {
			return;
		}

		Enumeration<String> headerNames = request.getHeaderNames();

		if (!headerNames.hasMoreElements()) {
			log.trace("No request headers ("+request.getRequestURI()+")");
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("Request headers ("+request.getRequestURI()+"): ");
			while (headerNames.hasMoreElements()) {
				String header = headerNames.nextElement();
				sb.append("\n\t").append("[").append(header).append("]: [").append(request.getHeader(header)).append("]");
			}
			log.trace(sb.toString());
		}

	}

	private static void logHeaders(HttpServletRequest request, HttpServletResponse response) {

		if (!log.isTraceEnabled()) {
			return;
		}

		Collection<String> headerNames = response.getHeaderNames();

		if (headerNames.isEmpty()) {
			log.trace("No response headers ("+request.getRequestURI()+")");
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("Response headers ("+request.getRequestURI()+"): ");
			for (String header : headerNames) {
				sb.append("\n\t").append("[").append(header).append("]: [").append(response.getHeader(header)).append("]");
			}
			log.trace(sb.toString());
		}

	}

}
