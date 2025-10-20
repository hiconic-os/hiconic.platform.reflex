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
package hiconic.rx.web.ddra.endpoints.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import com.braintribe.codec.marshaller.api.EntityRecurrenceDepth;
import com.braintribe.codec.marshaller.api.EntityVisitorOption;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.IdentityManagementMode;
import com.braintribe.codec.marshaller.api.IdentityManagementModeOption;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.exception.AuthorizationException;
import com.braintribe.mimetype.MimeTypeParser;
import com.braintribe.mimetype.ParsedMimeType;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.pr.criteria.matching.StandardMatcher;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.StandardCloningContext;
import com.braintribe.model.generic.reflection.StrategyOnCriterionMatch;
import com.braintribe.model.processing.securityservice.api.exceptions.SecurityServiceException;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.source.TransientSource;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.lcd.StringTools;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import hiconic.rx.web.ddra.endpoints.api.api.v1.ApiV1EndpointContext;
import hiconic.rx.webapi.endpoints.DdraEndpoint;
import hiconic.rx.webapi.endpoints.DdraEndpointDepth;
import hiconic.rx.webapi.endpoints.DdraEndpointDepthKind;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TODO this should be split up and wired into where it's needed...
 */
public class DdraEndpointsUtils {

	public static final String APPLICATION_JSON = "application/json";

	public static <T> T evaluateServiceRequest(Evaluator<ServiceRequest> evaluator, ServiceRequest service) {
		try {
			EvalContext<?> context = service.eval(evaluator);
			return (T) context.get();
		} catch (RuntimeException e) {
			// TODO handle other exceptions, check the root cause instead??
			if (e instanceof AuthorizationException || e instanceof SecurityServiceException) {
				HttpExceptions.throwUnauthorized(e.getMessage());
			}

			throw e;
		}
	}

	public static void writeResponse(DdraTraversingCriteriaMap criteriasMap, DdraEndpointContext<?> context, Object result, boolean full)
			throws IOException {
		writeResponse(criteriasMap, context, result, BaseType.INSTANCE, full);
	}

	public static void writeResponse(DdraTraversingCriteriaMap criteriasMap, DdraEndpointContext<?> context, Object result,
			GenericModelType expectedResponseType, boolean full) throws IOException {

		if (context.getForceResponseCode() == null && result == Neutral.NEUTRAL) {
			HttpServletResponse response = context.getResponse();
			response.setStatus(204); // No Content
			response.getOutputStream().close(); // Commit response
			return;
		}

		DdraEndpoint endpoint = context.getEndpoint();
		hiconic.rx.webapi.endpoints.OutputPrettiness endpointPrettiness = endpoint.getPrettiness();
		hiconic.rx.webapi.endpoints.TypeExplicitness endpointTypeExplicitness = endpoint.getTypeExplicitness();
		OutputPrettiness prettyness = (endpointPrettiness == null) ? null : OutputPrettiness.valueOf(endpointPrettiness.name());
		TypeExplicitness typeExplicitness = (endpointTypeExplicitness == null) ? null : TypeExplicitness.valueOf(endpointTypeExplicitness.name());

		// TODO .useDirectPropertyAccess(true) crashes everything.
		GmSerializationOptions options = GmSerializationOptions.defaultOptions.derive() //
				.outputPrettiness(prettyness) //
				.stabilizeOrder(context.getEndpoint().getStabilizeOrder()) //
				.writeEmptyProperties(endpoint.getWriteEmptyProperties()) //
				.writeAbsenceInformation(endpoint.getWriteAbsenceInformation()) //
				.set(TypeExplicitnessOption.class, typeExplicitness) //
				.set(EntityRecurrenceDepth.class, endpoint.getEntityRecurrenceDepth()) //
				.set(IdentityManagementModeOption.class, IdentityManagementMode.valueOf(endpoint.getIdentityManagementMode().name())) //
				.set(EntityVisitorOption.class, context.getMarshallingVisitor()).inferredRootType(expectedResponseType) //
				.build();

		Object toWrite = full ? result : getResultWithDepth(context.getEndpoint().getComputedDepth(), criteriasMap, result);

		try (OutputStream out = context.openResponseOutputStream()) {
			context.getMarshaller().marshall(out, toWrite, options);
		}
	}

	private static Object getResultWithDepth(DdraEndpointDepth depth, DdraTraversingCriteriaMap criteriasMap, Object result) {
		if (depth.getKind() == DdraEndpointDepthKind.reachable) {
			return result;
		}
		return cloneWithTraversingCriterion(criteriasMap.getCriterion(depth), result);
	}

	private static Object cloneWithTraversingCriterion(TraversingCriterion traversingCriterion, Object result) {
		StandardMatcher matcher = new StandardMatcher();
		matcher.setCriterion(traversingCriterion);

		StandardCloningContext cloningContext = new StandardCloningContext() {
			@Override
			public GenericEntity supplyRawClone(EntityType<? extends GenericEntity> entityType, GenericEntity entity) {
				GenericEntity clonedEntity = super.supplyRawClone(entityType, entity);

				if (entity.hasTransientData()) {
					if (entity instanceof TransientSource) {
						TransientSource transientSource = (TransientSource) entity;
						TransientSource clonedTransientSource = (TransientSource) clonedEntity;
						clonedTransientSource.setInputStreamProvider(transientSource.getInputStreamProvider());
					} else if (entity instanceof CallStreamCapture) {
						CallStreamCapture callStreamCapture = (CallStreamCapture) entity;
						CallStreamCapture clonedCallStreamCapture = (CallStreamCapture) clonedEntity;
						clonedCallStreamCapture.setOutputStreamProvider(callStreamCapture.getOutputStreamProvider());
					}
				}

				return clonedEntity;
			}
		};

		cloningContext.setMatcher(matcher);
		cloningContext.setAbsenceResolvable(true);

		return BaseType.INSTANCE.clone(cloningContext, result, StrategyOnCriterionMatch.partialize);
	}

	public static void computeDepth(DdraEndpoint endpoint) {
		DdraEndpointDepth depth = DdraEndpointDepth.T.create();
		endpoint.setComputedDepth(depth);
		if (DdraEndpointDepthKind.shallow.name().equals(endpoint.getDepth())) {
			depth.setKind(DdraEndpointDepthKind.shallow);
		} else if (DdraEndpointDepthKind.reachable.name().equals(endpoint.getDepth())) {
			depth.setKind(DdraEndpointDepthKind.reachable);
		} else {
			try {
				depth.setKind(DdraEndpointDepthKind.custom);
				depth.setCustomDepth(Integer.valueOf(endpoint.getDepth()));
			} catch (NumberFormatException e) {
				HttpExceptions.throwBadRequest("Invalid depth parameter, expected \"shallow\", \"reachable\" or a number > 0 but got %s",
						endpoint.getDepth());
			}

			if (depth.getCustomDepth() < 0) {
				HttpExceptions.throwBadRequest("Invalid depth parameter, expected \"shallow\", \"reachable\" or a number > 0 but got %s",
						endpoint.getDepth());
			}
		}
	}

	public static Marshaller getInMarshallerFor(MarshallerRegistry registry, DdraEndpoint endpoint) {
		return getInMarshallerFor(registry, endpoint.getContentType());
	}

	public static Marshaller getInMarshallerFor(MarshallerRegistry registry, String contentTypeParameter) {
		final String contentType;
		if (StringTools.isBlank(contentTypeParameter)) {
			// In some cases it's not possible to set the content type
			// - for example in a part of a form-data request when it comes from a native HTML form
			// Therefore we need a default
			// TODO: Maybe this is not the correct place to specify a default
			// If one is specified before we can throw an exception:
			// HttpExceptions.notAcceptable("Please specify a Content-Type header to unmarshall the body.");
			contentType = APPLICATION_JSON;
		} else {
			contentType = contentTypeParameter;
		}

		Marshaller marshaller = registry.getMarshaller(getMimeType(contentType));

		if (marshaller == null && contentTypeParameter != null) {
			String mimeType = getMimeType(contentTypeParameter);
			if ("text/plain".equalsIgnoreCase(mimeType)) {
				// This is a sensible fallback as a JSON part can also be sent as a text/plain part.
				marshaller = registry.getMarshaller(getMimeType(APPLICATION_JSON));
			}
		}

		if (marshaller == null)
			HttpExceptions.throwNotAcceptable("Unsupported Content-Type: %s.", contentTypeParameter);
		
		return marshaller;
	}

	public static <E extends DdraEndpoint> void computeOutMarshallerFor(MarshallerRegistry registry, DdraEndpointContext<E> context,
			String defaultMimeType) {
		AcceptHeaderResolver acceptHeaderResolver = new AcceptHeaderResolver(registry);
		acceptHeaderResolver.setInternalMimeTypeBias(APPLICATION_JSON);
		String mimeType = acceptHeaderResolver.resolveMimeType(context.getEndpoint().getAccept(), defaultMimeType);
		Marshaller marshaller = registry.getMarshaller(mimeType);

		context.setMimeType(mimeType);
		context.setMarshaller(marshaller);
	}

	/**
	 * Extract the mime type part from the content type header, since the given content type may include a character
	 * encoding specification, for example, {@code text/html;charset=UTF-8}.
	 * <p>
	 * In case the request content type contains further specialization, for example,
	 * {@code test/html;spec=module-checks-response}, the <code>spec</code> attribute is the only mime type attribute being
	 * retained.
	 */
	private static String getMimeType(String requestContentType) {
		if (requestContentType == null)
			return requestContentType;

		ParsedMimeType parsedType = MimeTypeParser.getParsedMimeType(requestContentType);
		parsedType.getParams().keySet().retainAll(Collections.singleton("spec"));

		return parsedType.toString();
	}

	public static String getPathInfo(ApiV1EndpointContext context) {
		String pathInfo = context.getRequest().getPathInfo() != null ? context.getRequest().getPathInfo() : "";

		if (pathInfo.startsWith("/")) {
			return pathInfo;
		}

		return "/" + pathInfo;
	}

	public static void setAllowHeader(DdraEndpointContext<?> context, Collection<String> allowedMethods) {
		String allowHeader = String.join(", ", allowedMethods);
		context.getResponse().addHeader("Allow", allowHeader);
	}
}
