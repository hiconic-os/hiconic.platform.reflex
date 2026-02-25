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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.GenericModelException;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import dev.hiconic.servlet.decoder.api.UrlPathCodec;
import dev.hiconic.servlet.decoder.impl.HttpRequestEntityDecoderUtils;
import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.web.rest.servlet.handlers.PathErrorHandler;
import hiconic.rx.web.rest.servlet.handlers.RestV2Handler;
import hiconic.rx.webapi.endpoints.v2.DdraUrlPathParameters;
import hiconic.rx.webapi.endpoints.v2.RestV2Endpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestV2Server extends AbstractDdraRestServlet<RestV2EndpointContext<RestV2Endpoint>> {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(RestV2Server.class);

	private static final String PROPERTIES_BASE_PATH = "properties";
	private static final String ENTITIES_BASE_PATH = "entities";
	private static final String SWAGGER_BASE_PATH = "swaggerentities";
	private static final String EXPORT_SWAGGER_BASE_PATH = "exportswaggerentities";
	private static final String SWAGGER_PROPERTIES_BASE_PATH = "swaggerproperties";
	private static final String EXPORT_SWAGGER_PROPERTIES_BASE_PATH = "exportswaggerproperties";

	public static final UrlPathCodec<GenericEntity> ENTITIES_URL_CODEC = UrlPathCodec.create() //
			.constantSegment(ENTITIES_BASE_PATH) //
			.mappedSegment("accessId") //
			.mappedSegment("typeSignature") //
			.mappedSegment("entityIdStringValue") //
			.mappedSegment("entityPartition");

	public static final UrlPathCodec<GenericEntity> PROPERTIES_URL_CODEC = UrlPathCodec.create() //
			.constantSegment(PROPERTIES_BASE_PATH) //
			.mappedSegment("accessId") //
			.mappedSegment("typeSignature") //
			.mappedSegment("entityIdStringValue") //
			.mappedSegment("entityPartition", true) //
			.mappedSegment("property");

	public static final UrlPathCodec<GenericEntity> SWAGGER_URL_CODEC = UrlPathCodec.create() //
			.constantSegment(SWAGGER_BASE_PATH) //
			.mappedSegment("accessId");

	public static final UrlPathCodec<GenericEntity> EXPORT_SWAGGER_URL_CODEC = UrlPathCodec.create() //
			.constantSegment(EXPORT_SWAGGER_BASE_PATH) //
			.mappedSegment("accessId");

	public static final UrlPathCodec<GenericEntity> SWAGGER_PROP_URL_CODEC = UrlPathCodec.create() //
			.constantSegment(SWAGGER_PROPERTIES_BASE_PATH) //
			.mappedSegment("accessId");

	public static final UrlPathCodec<GenericEntity> EXPORT_SWAGGER_PROP_URL_CODEC = UrlPathCodec.create() //
			.constantSegment(EXPORT_SWAGGER_PROPERTIES_BASE_PATH) //
			.mappedSegment("accessId");

	public static String getUrlFor(HttpServletRequest request, DdraUrlPathParameters parameters) {
		String encoded = parameters.getProperty() != null ? PROPERTIES_URL_CODEC.encode(parameters) : ENTITIES_URL_CODEC.encode(parameters);

		StringBuffer url = request.getRequestURL();
		int length = url.length();
		// +1 for the additional "/"
		url.replace(length - request.getPathInfo().length() + 1, length, encoded);

		return url.toString();
	}

	private Map<RestHandlerKey, RestV2Handler<?>> handlers;

	private AccessDomains accessDomains;

	// @formatter:off
	@Required public void setAccessDomains(AccessDomains accessDomains) { this.accessDomains = accessDomains; }
	@Required public void setHandlers(Map<RestHandlerKey, RestV2Handler<?>> handlers) { this.handlers = handlers; }
	// @formatter:on

	@Override
	protected void handle(RestV2EndpointContext<RestV2Endpoint> context) throws IOException {
		RestV2Handler<RestV2Endpoint> handler = context.getHandler();
		handler.handle(context);
	}

	private RestV2Handler<? extends RestV2Endpoint> getHandler(String url, String method) {
		RestV2Handler<? extends RestV2Endpoint> handler = handlers.get(new RestHandlerKey(method, url));

		if (handler != null)
			return handler;

		return new PathErrorHandler(marshallerRegistry,
				Reasons.build(InvalidArgument.T).text("Unsupported method " + method + " for path segment " + url).toMaybe());
	}

	@Override
	protected RestV2EndpointContext<RestV2Endpoint> createContext(HttpServletRequest request, HttpServletResponse response) {
		Maybe<CrudRequestTarget> maybeTarget = getRequestTarget(request);

		final RestV2Handler<? extends RestV2Endpoint> handler;
		final CrudRequestTarget target;

		if (maybeTarget.isUnsatisfied()) {
			handler = new PathErrorHandler(marshallerRegistry, maybeTarget);
			target = null;
		} else {
			target = maybeTarget.get();
			String url = target.getUrl();
			String method = request.getMethod();
			handler = getHandler(url, method);
		}

		RestV2EndpointContext<RestV2Endpoint> context = (RestV2EndpointContext<RestV2Endpoint>) handler.createContext(request, response);
		context.setEvaluator(evaluator);
		context.setTarget(target);
		context.setHandler((RestV2Handler<RestV2Endpoint>) handler);
		return context;
	}

	@Override
	protected boolean fillContext(RestV2EndpointContext<RestV2Endpoint> context) {
		computeUrlParameters(context);

		if (isSwaggerTarget(context))
			return true;

		checkAccess(context);
		computeEntityType(context);

		if (isSwaggerTarget(context))
			return true;

		computeEntityIdIfNecessary(context);
		computePropertyIfNecessary(context);

		return true;
	}

	private boolean isSwaggerTarget(RestV2EndpointContext<RestV2Endpoint> context) {
		return context.getTarget() == CrudRequestTarget.SWAGGER || context.getTarget() == CrudRequestTarget.EXPORT_SWAGGER
				|| context.getTarget() == CrudRequestTarget.EXPORT_SWAGGER_PROPERTIES || context.getTarget() == CrudRequestTarget.SWAGGER_PROPERTIES;
	}

	private void computeUrlParameters(RestV2EndpointContext<RestV2Endpoint> context) {
		DdraUrlPathParameters parameters = DdraUrlPathParameters.T.create();
		context.setParameters(parameters);
		String pathInfo = getPathInfo(context.getRequest());
		if (pathInfo == null) {
			return;
		}

		CrudRequestTarget target = context.getTarget();

		if (target == null)
			return;

		switch (target) {
			case ENTITY:
				ENTITIES_URL_CODEC.decode(() -> parameters, pathInfo);
				break;
			case PROPERTY:
				PROPERTIES_URL_CODEC.decode(() -> parameters, pathInfo);
				break;
			case SWAGGER:
				SWAGGER_URL_CODEC.decode(() -> parameters, pathInfo);
				break;
			case EXPORT_SWAGGER:
				EXPORT_SWAGGER_URL_CODEC.decode(() -> parameters, pathInfo);
				break;
			case SWAGGER_PROPERTIES:
				SWAGGER_PROP_URL_CODEC.decode(() -> parameters, pathInfo);
				break;
			case EXPORT_SWAGGER_PROPERTIES:
				EXPORT_SWAGGER_PROP_URL_CODEC.decode(() -> parameters, pathInfo);
				break;
		}
	}

	private void checkAccess(RestV2EndpointContext<RestV2Endpoint> context) {
		DdraUrlPathParameters parameters = context.getParameters();
		if (parameters.getAccessId() == null) {
			resolveTargetForSwagger(context);
			return;
		}

		if (!accessDomains.hasDomain(parameters.getAccessId()))
			HttpExceptions.throwNotFound("No access with accessId " + parameters.getAccessId() + " deployed");
	}

	private void resolveTargetForSwagger(RestV2EndpointContext<RestV2Endpoint> context) {
		if (context.getTarget() == CrudRequestTarget.ENTITY)
			context.setTarget(CrudRequestTarget.SWAGGER);
		else if (context.getTarget() == CrudRequestTarget.PROPERTY)
			context.setTarget(CrudRequestTarget.SWAGGER_PROPERTIES);
	}

	private void computeEntityType(RestV2EndpointContext<RestV2Endpoint> context) {
		DdraUrlPathParameters parameters = context.getParameters();
		String typeSignature = parameters.getTypeSignature();

		if (typeSignature == null) {
			resolveTargetForSwagger(context);
			return;
		}

		if (typeSignature.contains(".")) {
			try {
				context.setEntityType(EntityTypes.get(typeSignature));
			} catch (GenericModelException e) {
				HttpExceptions.throwNotFound("Entity type %s not found.", typeSignature);
			}
		} else {
			context.setEntityType(getBySimpleName(parameters));
		}
	}

	private EntityType<?> getBySimpleName(DdraUrlPathParameters parameters) {
		AccessDomain accessDomain = accessDomains.byId(parameters.getAccessId());

		ConfiguredModel dataModel = accessDomain.configuredDataModel();
		CmdResolver cmdResolver = dataModel.contextCmdResolver();

		String suffix = "." + parameters.getTypeSignature();
		List<EntityType<?>> types = cmdResolver.getModelOracle().getTypes().onlyEntities().filter(type -> type.getTypeSignature().endsWith(suffix))
				.<EntityType<?>> asTypes().collect(Collectors.toList());

		if (types.isEmpty()) {
			HttpExceptions.throwNotFound("Cannot find entity type with simple name %s in model %s", parameters.getTypeSignature(),
					dataModel.name());
		}
		if (types.size() > 1) {
			HttpExceptions.throwBadRequest("Found multiple (at least 2) entities with simple name %s in access %s: %s and %s",
					parameters.getTypeSignature(), parameters.getAccessId(), types.get(0).getTypeSignature(), types.get(1).getTypeSignature());
		}

		return types.get(0);
	}

	private void computeEntityIdIfNecessary(RestV2EndpointContext<RestV2Endpoint> context) {
		DdraUrlPathParameters parameters = context.getParameters();

		if (context.getTarget() == CrudRequestTarget.PROPERTY && parameters.getEntityIdStringValue() == null) {
			HttpExceptions.throwBadRequest(
					"Expected URL of the form /properties/accessId/entity.TypeSignature/id(/partition)/propertyName but the id was not specified.");
		}

		if (parameters.getEntityIdStringValue() != null) {
			ScalarType idType = getIdType(context);
			parameters.setEntityId(parse(idType, parameters.getEntityIdStringValue()));
		}
	}

	private ScalarType getIdType(RestV2EndpointContext<RestV2Endpoint> context) {
		CmdResolver cmdResolver = accessDomains.byId(context.getParameters().getAccessId()).configuredDataModel().contextCmdResolver();
		return cmdResolver.getIdType(context.getEntityType().getTypeSignature());
	}

	private void computePropertyIfNecessary(RestV2EndpointContext<RestV2Endpoint> context) {
		CrudRequestTarget target = context.getTarget();

		if (target == null)
			return;

		if (target == CrudRequestTarget.ENTITY) {
			return;
		}

		DdraUrlPathParameters parameters = context.getParameters();
		if (parameters.getProperty() == null) {
			HttpExceptions.throwBadRequest(
					"Expected URL of the form /properties/accessId/entity.TypeSignature/id(/partition)/propertyName but the propertyName was not specified.");
		}

		Property property = context.getEntityType().findProperty(parameters.getProperty());
		if (property == null) {
			HttpExceptions.throwNotFound("No property with name %s found in entityType %s.", parameters.getProperty(),
					context.getEntityType().getTypeSignature());
		}

		context.setProperty(property);
	}

	private Maybe<CrudRequestTarget> getRequestTarget(HttpServletRequest request) {
		String path = getPathInfo(request);

		if (path == null) {
			path = ENTITIES_BASE_PATH;
		}

		int index = path.indexOf('/');

		if (index == -1)
			index = path.length();

		String selector = path.substring(0, index);

		final CrudRequestTarget target;

		switch (selector) {
			case ENTITIES_BASE_PATH:
				target = CrudRequestTarget.ENTITY;
				break;
			case PROPERTIES_BASE_PATH:
				target = CrudRequestTarget.PROPERTY;
				break;
			default:
				target = null;
		}

		if (target != null)
			return Maybe.complete(target);

		return Reasons.build(InvalidArgument.T) //
				.text("Expected URL path of the form entities/... or properties/... but got: " + path) //
				.toMaybe();
	}

	private String getPathInfo(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			return null;
		}
		return pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
	}

	private Object parse(ScalarType type, String encodedValue) {
		switch (type.getTypeCode()) {
			case booleanType:
				return Boolean.parseBoolean(encodedValue);
			case dateType:
				return HttpRequestEntityDecoderUtils.parseDate(encodedValue);
			case decimalType:
				return new BigDecimal(encodedValue);
			case doubleType:
				return Double.parseDouble(encodedValue);
			case stringType:
				return encodedValue;
			case floatType:
				return Float.parseFloat(encodedValue);
			case integerType:
				return Integer.parseInt(encodedValue);
			case longType:
				return Long.parseLong(encodedValue);
			case enumType:
				return ((EnumType<?>) type).getEnumValue(encodedValue);
			default:
				HttpExceptions.throwBadRequest("Unsupported ID type %s", type.getTypeName());
				return null;
		}
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
