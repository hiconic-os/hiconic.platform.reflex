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
package hiconic.rx.security.web.processing.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.common.Waypoint;
import com.braintribe.exception.HttpException;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.gm.model.security.reason.SecurityReason;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.aspect.RequestedEndpointAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorAddressAspect;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

import dev.hiconic.servlet.api.remote.RemoteAddressInformation;
import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import dev.hiconic.servlet.impl.remote.DefaultRemoteClientAddressResolver;
import hiconic.rx.security.web.api.CookieHandler;
import hiconic.rx.security.web.processing.servlet.aspect.AuthHttpRequestSupplier;
import hiconic.rx.security.web.processing.servlet.aspect.AuthHttpRequestSupplierAspect;
import hiconic.rx.security.web.processing.servlet.aspect.AuthHttpRequestSupplierImpl;
import hiconic.rx.security.web.processing.servlet.aspect.AuthHttpResponseConfigurer;
import hiconic.rx.security.web.processing.servlet.aspect.AuthHttpResponseConfigurerAspect;
import hiconic.rx.security.web.processing.servlet.aspect.AuthHttpResponseConfigurerImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthRxServlet extends HttpServlet {

	private static final long serialVersionUID = -3371378397236984055L;

	public final static String TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED = "TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED";

	private final Logger log = Logger.getLogger(AuthRxServlet.class);

	private RemoteClientAddressResolver remoteAddressResolver;
	private MarshallerRegistry marshallerRegistry;
	private CookieHandler cookieHandler;
	private Evaluator<ServiceRequest> requestEvaluator;
	private Function<HttpServletRequest, String> entryPointProvider = r -> null;

	@Configurable
	public void setEntryPointProvider(Function<HttpServletRequest, String> entryPointProvider) {
		this.entryPointProvider = entryPointProvider;
	}
	
	@Configurable
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}

	@Configurable
	public void setRemoteAddressResolver(RemoteClientAddressResolver remoteAddressResolver) {
		this.remoteAddressResolver = remoteAddressResolver;
	}
	public RemoteClientAddressResolver getRemoteAddressResolver() {
		if (remoteAddressResolver == null) {
			remoteAddressResolver = DefaultRemoteClientAddressResolver.getDefaultResolver();
		}
		return remoteAddressResolver;
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Create authentication request.

		OpenUserSessionWithUserAndPassword authRequest = unmarshallRequest(req);

		AttributeContext attributeContext = buildAttributeContext(req, authRequest);
		
		OpenUserSession openUserSession = createOpenUserSession(req, authRequest);

		AttributeContexts.push(attributeContext);
		try {

			Maybe<UserSession> sessionMaybe = authenticate(resp, openUserSession);

			consumeResponseInAspect(sessionMaybe, attributeContext, resp);

			if (sessionMaybe.isUnsatisfied()) {

				Reason whyUnsatisfied = sessionMaybe.whyUnsatisfied();

				if (!sessionMaybe.isUnsatisfiedAny(SecurityReason.T, InvalidArgument.T)) {
					String logToken = UUID.randomUUID().toString();
					log.warn(logToken + ": " + whyUnsatisfied.stringify());
					whyUnsatisfied = Reasons.build(InternalError.T).text("Please check the log files and search for error ID: " + logToken)
							.toReason();
				}

				Marshaller marshaller = marshallerRegistry.getMarshaller("application/json");
				resp.setContentType("application/json");
				
				resp.setStatus(getStatus(whyUnsatisfied));
				marshaller.marshall(resp.getOutputStream(), whyUnsatisfied);
				return;
			}
			UserSession session = sessionMaybe.get();

			String sessionId = session.getSessionId();
			log.debug("Successfully authenticated user: " + authRequest.getUser() + " with session: " + sessionId);

			cookieHandler.ensureCookie(req, resp, sessionId, authRequest.getStaySignedIn());

		} finally {
			AttributeContexts.pop();
		}
	}

	private OpenUserSession createOpenUserSession(HttpServletRequest req, OpenUserSessionWithUserAndPassword authRequest) {
		String entryPoint = entryPointProvider.apply(req);
		
		UserPasswordCredentials credentials = UserPasswordCredentials.forUserName(authRequest.getUser(), authRequest.getPassword());
		
		OpenUserSession openUserSession = OpenUserSession.T.create();
		openUserSession.setCredentials(credentials);
		openUserSession.setLocale(authRequest.getLocale());
		openUserSession.setEntryPoint(entryPoint);
		return openUserSession;
	}

	private int getStatus(Reason whyUnsatisfied) {
		if (whyUnsatisfied instanceof AuthenticationFailure)
			return HttpServletResponse.SC_UNAUTHORIZED;
		else if (whyUnsatisfied instanceof Forbidden)
			return HttpServletResponse.SC_FORBIDDEN;
		else if (whyUnsatisfied instanceof InvalidArgument) {
			return HttpServletResponse.SC_BAD_REQUEST;
		}
		else
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}

	private void consumeResponseInAspect(Maybe<UserSession> sessionMaybe, AttributeContext attributeContext, HttpServletResponse resp) {
		AuthHttpResponseConfigurer responseConfigurer = attributeContext.findOrNull(AuthHttpResponseConfigurerAspect.class);
		if (responseConfigurer instanceof AuthHttpResponseConfigurerImpl) {
			AuthHttpResponseConfigurerImpl impl = (AuthHttpResponseConfigurerImpl) responseConfigurer;
			if (sessionMaybe.isSatisfied()) {
				Object result = sessionMaybe.get();
				impl.consume(result, resp);
			} else {
				impl.consume(null, resp);
			}
		}

	}

	public Marshaller getMarshaller(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType == null) {
			contentType = "application/json";
		}
		String mimeType = getMimeType(contentType);
		Marshaller marshaller = marshallerRegistry.getMarshaller(mimeType);
		if (marshaller != null) {
			return marshaller;
		}
		throw new HttpException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Unsupported Content-Type.");
	}

	private static String getMimeType(String requestContentType) {

		if (requestContentType == null)
			return requestContentType;

		if (requestContentType.indexOf(";") == -1)
			return requestContentType;

		return requestContentType.substring(0, requestContentType.indexOf(";")).trim();
	}

	protected static boolean offerStaySigned() {
		return true;
//		String offerStayLoggedIn = TribefireRuntime.getProperty(TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED);
//		if (offerStayLoggedIn != null && offerStayLoggedIn.equalsIgnoreCase("false")) {
//			return false;
//		} else {
//			return true;
//		}
	}

	private Maybe<UserSession> authenticate(@SuppressWarnings("unused") HttpServletResponse resp, OpenUserSession request) {

		EvalContext<? extends OpenUserSessionResponse> responseContext = request.eval(requestEvaluator);
		Maybe<? extends OpenUserSessionResponse> reasonedResponse = responseContext.getReasoned();

		if (!reasonedResponse.isSatisfied()) {
			return reasonedResponse.whyUnsatisfied().asMaybe();
		}

		OpenUserSessionResponse response = reasonedResponse.get();

		return Maybe.complete(response.getUserSession());
	}

	protected OpenUserSessionWithUserAndPassword unmarshallRequest(HttpServletRequest request) {
		
		try (InputStream in = request.getInputStream()) {
			// 3. Unmarshall the request from the body
			Marshaller marshaller = getMarshaller(request);
			OpenUserSessionWithUserAndPassword authRequest = (OpenUserSessionWithUserAndPassword) marshaller.unmarshall(in,
					GmDeserializationOptions.deriveDefaults().setInferredRootType(OpenUserSessionWithUserAndPassword.T).build());
			
			if (authRequest == null) {
				throw new HttpException(HttpServletResponse.SC_BAD_REQUEST, "Could not decode credentials from request");
			}
			
			Locale locale = request.getLocale();
			if (locale != null) {
				authRequest.setLocale(locale.getLanguage());
			}

			logAuthRequest(request, authRequest);
			
			return authRequest;
			
		} catch (IOException ioe) {
			throw new HttpException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not ready request body", ioe);
		}
		
	}
	
	protected void logAuthRequest(HttpServletRequest request, OpenUserSessionWithUserAndPassword authRequest) {
		String user = authRequest.getUser();
		RemoteClientAddressResolver resolver = getRemoteAddressResolver();
		try {
			RemoteAddressInformation remoteAddressInformation = resolver.getRemoteAddressInformation(request);
			String remoteAddress = remoteAddressInformation.getRemoteIp();
			log.info("Received an authentication request for user '" + user + "' from [" + remoteAddress + "]. Remote Address Information: "
					+ remoteAddressInformation.toString());
		} catch (Exception e) {
			String message = "Could not use the client address resolver to get the client's IP address. User: '" + user + "'";
			log.info(message);
			if (log.isDebugEnabled())
				log.debug(message, e);
		}
	}
	
	/**
	 * <p>
	 * Retrieves the client's remote Internet protocol address.
	 * 
	 * @param request
	 *            The request from the client.
	 * @return The remote address of the client.
	 */
	private String getClientRemoteInternetAddress(HttpServletRequest request) {

		if (remoteAddressResolver == null) {
			remoteAddressResolver = DefaultRemoteClientAddressResolver.getDefaultResolver();
		}

		return remoteAddressResolver.getRemoteIpLenient(request);

	}

	protected AttributeContext buildAttributeContext(HttpServletRequest httpRequest, OpenUserSessionWithUserAndPassword authRequest) {
		AuthHttpRequestSupplier httpRequestSupplier = new AuthHttpRequestSupplierImpl(authRequest, httpRequest);
		AuthHttpResponseConfigurerImpl httpResponseConfigurer = new AuthHttpResponseConfigurerImpl();

		//@formatter:off
		return AttributeContexts.peek().derive()
				.set(RequestedEndpointAspect.class, httpRequest.getRequestURL().toString())
				.set(RequestorAddressAspect.class, getClientRemoteInternetAddress(httpRequest))
				.set(AuthHttpRequestSupplierAspect.class, httpRequestSupplier)
				.set(AuthHttpResponseConfigurerAspect.class, httpResponseConfigurer)
				.set(Waypoint.class, "platform-login")
				.build();
		//@formatter:on
	}

	@Required
	@Configurable
	public void setRequestEvaluator(Evaluator<ServiceRequest> requestEvaluator) {
		this.requestEvaluator = requestEvaluator;
	}
	@Configurable
	@Required
	public void setCookieHandler(CookieHandler cookieHandler) {
		this.cookieHandler = cookieHandler;
	}

}
