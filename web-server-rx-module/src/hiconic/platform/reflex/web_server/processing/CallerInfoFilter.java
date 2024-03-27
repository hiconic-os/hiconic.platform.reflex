// ============================================================================
package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.braintribe.cfg.Configurable;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.common.CallerAcceptedLocales;
import com.braintribe.model.processing.service.api.aspect.RequestedEndpointAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorAddressAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorIdAspect;
import com.braintribe.util.servlet.HttpFilter;
import com.braintribe.util.servlet.HttpServletArguments;
import com.braintribe.util.servlet.HttpServletArgumentsAttribute;
import com.braintribe.util.servlet.remote.DefaultRemoteClientAddressResolver;
import com.braintribe.util.servlet.remote.RemoteClientAddressResolver;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.collection.impl.AttributeContexts;

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
