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
package hiconic.rx.web.rest.servlet;

import java.io.IOException;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.common.lcd.Pair;
import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.processing.service.api.aspect.RequestorAddressAspect;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.StopWatch;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import dev.hiconic.servlet.impl.util.ServletTools;
import hiconic.rx.web.ddra.endpoints.api.DdraEndpointContext;
import hiconic.rx.web.ddra.endpoints.api.DdraEndpointsUtils;
import hiconic.rx.web.ddra.endpoints.api.DdraTraversingCriteriaMap;
import hiconic.rx.web.ddra.endpoints.api.v1.ApiV1EndpointContext;
import hiconic.rx.webapi.endpoints.DdraEndpoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractDdraRestServlet<Context extends DdraEndpointContext<? extends DdraEndpoint>> extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected Evaluator<ServiceRequest> evaluator;

	protected MarshallerRegistry marshallerRegistry;

	protected DdraEndpointsExceptionHandler exceptionHandler;

	protected DdraTraversingCriteriaMap traversingCriteriaMap;

	protected void writeResponse(ApiV1EndpointContext context, Object result, DdraEndpoint endpoint, boolean full) throws IOException {
		GenericModelType responseType;
		if (endpoint.getInferResponseType()) {
			responseType = GMF.getTypeReflection().getType(result);
		} else {
			responseType = BaseType.INSTANCE;
		}
		
		writeResponse(context, result, responseType, full);
	}
	
	protected void writeResponse(ApiV1EndpointContext context, Object result, GenericModelType responseType, boolean full) throws IOException {
		DdraEndpointsUtils.writeResponse(traversingCriteriaMap, context, result, responseType, full);
	}

	protected Marshaller getInMarshallerFor(DdraEndpoint endpoint) {
		return DdraEndpointsUtils.getInMarshallerFor(marshallerRegistry, endpoint);
	}

	protected Marshaller getInMarshallerFor(String contentType) {
		return DdraEndpointsUtils.getInMarshallerFor(marshallerRegistry, contentType);
	}

	protected <E extends DdraEndpoint> void computeOutMarshallerFor(DdraEndpointContext<E> context, String defaultMimeType) {
		DdraEndpointsUtils.computeOutMarshallerFor(marshallerRegistry, context, defaultMimeType);
	}

	protected abstract Logger getLogger();

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Pair<String, Boolean> requestLogAndRecurring = logRequest(request);
		String requestLog = requestLogAndRecurring.first;

		Context context = createContext(request, response);

		StopWatch stopWatch = context.getStopWatch();
		stopWatch.intermediate("Context creation");

		Logger logger = getLogger();
		try {
			if (fillContext(context)) {
				stopWatch.intermediate("Context filled");
				handle(context);
				stopWatch.intermediate("Request handling");
			}
		} catch (Exception e) {

			// There is not really a nice way to detect if a client has aborted the request
			// and/or the connection has been dropped. However, we do not want to show a full
			// blown exception in the server when it did nothing wrong.
			// We check therefore the first 3 levels of exception causes and see if there is a
			// ClientAbortException somewhere. I picked the first 3 levels because it seems to
			// be a good compromise. If taking less, we may miss it because of some other stupid wrappers.
			// If we take more, we might find a false positive (e.g., when a service made
			// a remote call and got an ClientAbortException this way... unlikely, but who knows).
			boolean abortFound = false;
			Throwable t = e;
			for (int i = 0; i < 3 && t != null; ++i) {
				String message = t.getMessage();
				if (message != null && message.contains("ClientAbortException")) {
					abortFound = true;
					break;
				}
				t = t.getCause();
			}

			if (abortFound) {
				logger.debug(() -> "Request has been aborted by client: " + requestLog);
			} else {
				logger.debug(() -> "Error while executing: " + requestLog + ". " + e.getMessage(), e);
				throw Exceptions.unchecked(e, requestLog);
			}

		} finally {
			if (requestLogAndRecurring.second) {
				logger.trace(() -> requestLog + " has been executed: " + stopWatch.toString());
			} else {
				logger.debug(() -> requestLog + " has been executed: " + stopWatch.toString());
			}
		}
	}

	protected abstract Context createContext(HttpServletRequest request, HttpServletResponse response);

	/**
	 * TODO: This method seems to exist for the sole purpose of being able to stop the time the "filling" needs. See if we
	 * can solve this another way...
	 */
	protected abstract boolean fillContext(Context context);

	protected void handle(Context context) throws IOException {
		switch (context.getRequest().getMethod()) {
			case "GET":
				handleGet(context);
				break;
			case "POST":
				handlePost(context);
				break;
			case "PUT":
				handlePut(context);
				break;
			case "PATCH":
				handlePatch(context);
				break;
			case "DELETE":
				handleDelete(context);
				break;
			default:
				// TODO handle OPTIONS and search HEAD and TRACE
				unsupportedMethod(context);
		}
	}

	@SuppressWarnings("unused")
	protected void handleGet(Context context) throws IOException {
		unsupportedMethod(context);
	}

	@SuppressWarnings("unused")
	protected void handlePost(Context context) throws IOException {
		unsupportedMethod(context);
	}

	@SuppressWarnings("unused")
	protected void handlePut(Context context) throws IOException {
		unsupportedMethod(context);
	}

	@SuppressWarnings("unused")
	protected void handlePatch(Context context) throws IOException {
		unsupportedMethod(context);
	}

	@SuppressWarnings("unused")
	protected void handleDelete(Context context) throws IOException {
		unsupportedMethod(context);
	}

	@SuppressWarnings("unused")
	protected void handleOptions(Context context) throws IOException {
		unsupportedMethod(context);
	}

	private void unsupportedMethod(Context context) {
		HttpExceptions.throwMethodNotAllowed("Unsupported method: \"$s\"", context.getRequest().getMethod());
	}

	private Pair<String, Boolean> logRequest(HttpServletRequest request) {

		StringBuilder sb = new StringBuilder(ServletTools.stringify(request));

		String requestorAddress = AttributeContexts.peek().findOrNull(RequestorAddressAspect.class);
		if (requestorAddress != null) {
			sb.append(" (From: ");
			sb.append(requestorAddress);
			sb.append(")");
		}

		final String msg = "REST call: " + sb.toString();

		String requestURI = request.getRequestURI();
		boolean isHealthz = requestURI.endsWith("api/v1/healthz");
		if (isHealthz) {
			getLogger().trace(() -> msg);
		} else {
			getLogger().debug(() -> msg);
		}

		return new Pair<>(msg, isHealthz);
	}

	@Required
	@Configurable
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}

	@Required
	@Configurable
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}

	@Required
	@Configurable
	public void setExceptionHandler(DdraEndpointsExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	
	@Required
	@Configurable
	public void setTraversingCriteriaMap(DdraTraversingCriteriaMap traversingCriteriaMap) {
		this.traversingCriteriaMap = traversingCriteriaMap;
	}
}
