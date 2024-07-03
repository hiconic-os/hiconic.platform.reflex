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

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import com.braintribe.cfg.Configurable;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.common.CallerAcceptedLocales;
import com.braintribe.model.processing.service.api.aspect.RequestedEndpointAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorAddressAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorIdAspect;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.collection.impl.AttributeContexts;

import dev.hiconic.servlet.api.HttpServletArguments;
import dev.hiconic.servlet.api.HttpServletArgumentsAttribute;
import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import dev.hiconic.servlet.impl.remote.DefaultRemoteClientAddressResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Smartly extracts client ip address from special headers to avoid proxy masking of remote address and pushes an
 * {@link AttributeContext} with {@link RequestorAddressAspect} set
 */
public class CallerInfoFilter implements HttpFilter {

	private RemoteClientAddressResolver remoteAddressResolver;

	@Configurable
	public void setRemoteAddressResolver(RemoteClientAddressResolver remoteClientAddressResolver) {
		this.remoteAddressResolver = remoteClientAddressResolver;
	}

	/**
	 * <p>
	 * Retrieves the client's remote Internet protocol address.
	 */
	protected String getClientRemoteInternetAddress(HttpServletRequest request) {

		if (remoteAddressResolver == null) {
			remoteAddressResolver = DefaultRemoteClientAddressResolver.getDefaultResolver();
		}

		return remoteAddressResolver.getRemoteIpLenient(request);
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

		String requestorId = request.getHeader("gm-rpc-client-id");
		String requestedEndpoint = request.getRequestURL().toString();

		AttributeContext attributeContext = AttributeContexts.peek();
		AttributeContext derivedContext = attributeContext.derive() //
				.set(RequestorAddressAspect.class, getClientRemoteInternetAddress(request)) //
				.set(CallerAcceptedLocales.class, getLocaleList(request)) //
				.set(RequestorIdAspect.class, requestorId) //
				.set(RequestedEndpointAspect.class, requestedEndpoint) //
				.set(HttpServletArgumentsAttribute.class, new HttpServletArguments(request, response)).build();

		AttributeContexts.push(derivedContext);

		try {
			chain.doFilter(request, response);
		} finally {

			AttributeContexts.pop();
		}
	}

	private List<Locale> getLocaleList(HttpServletRequest request) {
		// Note: the request.getLocales() method also returns the server locale if the client has not sent any Accept-Language
		// header. Thus, following processes might think that the one accepted locale that is always returned has been sent by
		// the client. For this reason, we assume an empty list of not a single Accept-Language header has been found.
		String acceptLanguageHeader = request.getHeader("Accept-Language");
		if (StringTools.isBlank(acceptLanguageHeader)) {
			return Collections.emptyList();
		}

		Enumeration<Locale> localesEnum = request.getLocales();

		final List<Locale> localeList;

		if (localesEnum != null) {
			localeList = Collections.list(localesEnum);
		} else {
			localeList = Collections.emptyList();
		}
		return localeList;
	}
}
