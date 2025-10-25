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
package hiconic.rx.openapi.v3.processing.processor.export;

import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentMap;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.CollectionTools2.newSet;
import static hiconic.rx.openapi.v3.processing.processor.export.OpenapiMimeType.APPLICATION_JSON;
import static hiconic.rx.openapi.v3.processing.processor.export.OpenapiMimeType.MULTIPART_FORMDATA;
import static hiconic.rx.openapi.v3.processing.processor.export.OpenapiMimeType.URLENCODED;
import static hiconic.rx.webapi.common.MetadataUtils.description;
import static hiconic.rx.webapi.common.MetadataUtils.name;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.generic.mdec.ModelDeclaration;
import com.braintribe.model.generic.reflection.CustomType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.generic.reflection.EntityVisitor;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.data.mapping.UnsatisfiedBy;
import com.braintribe.model.meta.selector.UseCaseSelector;
import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.openapi.v3_0.OpenapiOperation;
import com.braintribe.model.openapi.v3_0.OpenapiParameter;
import com.braintribe.model.openapi.v3_0.OpenapiPath;
import com.braintribe.model.openapi.v3_0.OpenapiRequestBody;
import com.braintribe.model.openapi.v3_0.OpenapiTag;
import com.braintribe.model.openapi.v3_0.api.OpenapiServicesRequest;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.AuthorizedRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMapping;
import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMappingImpl;
import hiconic.rx.web.ddra.endpoints.api.v1.WebApiMappingOracle;
import hiconic.rx.webapi.common.MetadataUtils;
import hiconic.rx.webapi.endpoints.api.v1.ApiV1DdraEndpoint;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;

public class ApiV1OpenapiProcessor extends AbstractOpenapiProcessor<OpenapiServicesRequest> {

	private ServiceDomains serviceDomains;
	private WebApiMappingOracle mappingOracle;
	private String relativeEndpointPath = "api/";

	private final Map<String, Set<String>> domainIdToMdUseCases = newConcurrentMap();

	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	@Required
	public void setWebApiMappingOracle(WebApiMappingOracle mappingOracle) {
		this.mappingOracle = mappingOracle;
	}

	@Configurable
	public void setWebApiServletPath(String webApiServletPath) {
		this.relativeEndpointPath = NullSafe.nonNull(webApiServletPath, "webApiServletPath") + "/";
	}

	@Override
	protected void init() {
		// nothing to do
	}

	@Override
	protected void process(OpenapiContext sessionScopedContext, OpenapiServicesRequest request, OpenApi openApi) {
		Map<String, OpenapiPath> paths = openApi.getPaths();
		Set<String> ddraMapedTypes = newSet();
		Map<String, OpenapiPath> pathStringToPath = new HashMap<>();
		Set<String> encounteredTags = newSet();
		Map<String, String> modelNameToTag = newMap();

		String modelDescription = description(sessionScopedContext.getMetaData()).atModel();
		if (modelDescription != null) {
			openApi.getInfo().setDescription(modelDescription);
		}

		String modelName = name(sessionScopedContext.getMetaData()).atModel();
		if (modelName != null) {
			openApi.getInfo().setTitle(modelName);
		}

		ComponentScope sessionEndpointScope = new ComponentScope(sessionScopedContext.getComponentScope(),
				standardComponentsContext.getComponentScope().getCmdResolver());
		OpenapiContext endpointParametersResolvingContext = standardComponentsContext.childContext("-ENDPOINTS-SESSION", sessionEndpointScope,
				URLENCODED);
		endpointParametersResolvingContext.transferRequestDataFrom(sessionScopedContext);

		mappingOracle.getAllForDomain(request.getServiceDomain()).stream() //
				.sorted(Comparator.comparing(SingleDdraMapping::getPathInfo).thenComparing(SingleDdraMapping::getMethod)).forEach(m -> {
					OpenapiPath path = pathStringToPath.computeIfAbsent(m.getPathInfo(), pathInfo -> {
						OpenapiPath p = OpenapiPath.T.create();
						return p;
					});

					OpenapiContext pathRequestResolvingContext = pathContext(sessionScopedContext, m.getPathInfo(), m);
					OpenapiContext pathEndpointParametersResolvingContext = pathContext(endpointParametersResolvingContext, m.getPathInfo(), m);

					createAnyOperation(pathRequestResolvingContext, pathEndpointParametersResolvingContext, m, path);

					m.getTags().forEach(encounteredTags::add);

					ddraMapedTypes.add(m.getRequestType().getTypeSignature());

					paths.put(m.getPathInfo(), path);
				});

		// in simple usecase generic endpoints are not shown if there are any explicitly mapped ones
		if (paths.isEmpty() || !isSimpleView(sessionScopedContext)) {
			List<EntityType<?>> requestTypes = modelEntities(sessionScopedContext) //
					.filter(et -> !et.isAbstract()) //
					.filter(et -> ServiceRequest.T.isAssignableFrom(et)) //
					.filter(et -> !ddraMapedTypes.contains(et.getTypeSignature())) //
					.collect(Collectors.toList());

			Map<String, List<EntityType<?>>> typesByShortName = requestTypes.stream().collect(Collectors.groupingBy( //
					EntityType::getShortName, //
					Collectors.toList()) //
			);

			requestTypes.stream() //
					.sorted(Comparator //
							.comparing((EntityType<?> t) -> MetadataUtils.priority(sessionScopedContext.getMetaData().entityType(t)).atEntity()) //
							.thenComparing(et -> shortNameIfUniqueOrFullSignature(typesByShortName, et))) //
					.forEach(t -> {
						boolean needsFullName = !shortNameIsUnique(typesByShortName, t);

						String fullKey = "/" + request.getServiceDomain() + getPathKey("", t, "", true);
						String maybeShortKey = "/" + request.getServiceDomain() + getPathKey("", t, "", needsFullName);

						OpenapiContext pathRequestResolvingContext = sessionScopedContext;
						OpenapiContext pathEndpointParametersResolvingContext = endpointParametersResolvingContext;

						if (needsPathSpecificContext(request, fullKey)) {
							pathRequestResolvingContext = pathContext(sessionScopedContext, fullKey);
							pathEndpointParametersResolvingContext = pathContext(pathEndpointParametersResolvingContext, fullKey);
						}

						String tag = acquireTagName(modelNameToTag, t);
						paths.put(maybeShortKey,
								createGenericPath(pathRequestResolvingContext, pathEndpointParametersResolvingContext, t, maybeShortKey, tag));
					});
		}

		encounteredTags.stream() //
				.sorted() //
				.forEach(tag -> addNewTag(openApi, tag));

		// We use models from ModelOracle as it lists them in a depender-first order
		ModelOracle modelOracle = sessionScopedContext.getComponentScope().getModelOracle();
		modelOracle.getDependencies() //
				.transitive() //
				.includeSelf() //
				.asGmMetaModels() //
				.map(gmModel -> modelNameToTag.get(gmModel.getName())) //
				.filter(tag -> tag != null) //
				.forEach(tag -> addNewTag(openApi, tag));
	}

	private String shortNameIfUniqueOrFullSignature(Map<String, List<EntityType<?>>> typesBySimpleName, EntityType<?> et) {
		return shortNameIsUnique(typesBySimpleName, et) ? et.getShortName() : et.getTypeSignature();
	}

	private boolean shortNameIsUnique(Map<String, List<EntityType<?>>> typesBySimpleName, EntityType<?> et) {
		List<EntityType<?>> types = typesBySimpleName.get(et.getShortName());
		return types.size() == 1;
	}

	private String acquireTagName(Map<String, String> modelNameToTag, EntityType<?> t) {
		Model m = t.getModel();
		return modelNameToTag.computeIfAbsent(m.name(), n -> {
			ModelDeclaration d = m.getModelArtifactDeclaration();
			return d.getArtifactId() + " (" + d.getGroupId() + ")";
		});
	}

	private void addNewTag(OpenApi openApi, String name) {
		OpenapiTag tag = OpenapiTag.T.create();
		tag.setName(name);

		openApi.getTags().add(tag);
	}

	private boolean needsPathSpecificContext(OpenapiServicesRequest request, String path) {
		return useCaseExistsFor(request, path);
	}

	private boolean useCaseExistsFor(OpenapiServicesRequest request, String openapiUseCase) {
		String domainId = request.getServiceDomain();

		Set<String> useCases = domainIdToMdUseCases.computeIfAbsent(domainId, dId -> computeUseCasesForDomain(dId));

		return useCases.contains("ddra:" + openapiUseCase) || useCases.contains("openapi:" + openapiUseCase);
	}

	private Set<String> computeUseCasesForDomain(String domainId) {
		GmMetaModel gmModel = requireServiceDomain(domainId).configuredModel().modelOracle().getGmMetaModel();

		Set<String> result = newSet();

		GmMetaModel.T.traverse(gmModel, null, EntityVisitor.onVisitEntity((entity, criterion, context) -> {
			if (entity instanceof UseCaseSelector ucs)
				result.add(ucs.getUseCase());
		}));

		return result;
	}

	private OpenapiContext pathContext(OpenapiContext context, String path) {
		return pathContext(context, path, null);
	}

	private OpenapiContext pathContext(OpenapiContext context, String path, SingleDdraMapping mapping) {
		String keySuffix = path.replaceAll("['\"/~]", "_");
		OpenapiContext pathContext = context.childContext(keySuffix);
		pathContext.setUseCasesForDdraAndOpenapi(path);
		pathContext.setMapping(mapping);
		return pathContext;
	}

	private String summaryFromTypeSignature(CustomType requestType) {
		String shortTypeName = requestType.getShortName();

		return shortTypeName.replaceAll("([A-Z])", " $1").substring(1);
	}

	private void createAnyOperation(OpenapiContext requestResolvingContext, OpenapiContext endpointParametersResolvingContext,
			SingleDdraMapping mapping, OpenapiPath path) {
		EntityType<?> requestType = mapping.getRequestType();
		String keySuffix = "-" + mapping.getMethod();
		OpenapiContext context = requestResolvingContext.childContext(keySuffix);
		GenericModelType responseType = context.getEntityTypeOracle(requestType).getEvaluatesTo().get().reflectionType();
		boolean isAuthorizedRequest = AuthorizedRequest.T.isAssignableFrom(requestType);

		List<EntityType<?>> potentialReasonTypes = collectPotentialReasons(requestType, context);
		OpenapiOperation operation = createOperation(context, responseType, isAuthorizedRequest, potentialReasonTypes);

		EntityMdResolver requestTypeMdResolver = requestResolvingContext.getMetaData().entityType(requestType);
		String requestEntityDescription = description(requestTypeMdResolver).atEntity();
		String requestEntityName = MetadataUtils.name(requestTypeMdResolver).atEntity();

		String description = "Mapped endpoint for <b>" + requestType.getTypeSignature() + "</b><br>";

		if (requestEntityDescription != null)
			description += requestEntityDescription;

		String summary = requestEntityName;

		if (summary == null) {
			summary = summaryFromTypeSignature(requestType);
		}

		operation.setDescription(description);
		operation.setSummary(summary);

		switch (mapping.getMethod()) {
			case GET:
				createGetOperation(operation, requestResolvingContext, mapping);
				path.setGet(operation);
				break;
			case DELETE:
				createDeleteOperation(operation, requestResolvingContext, mapping);
				path.setDelete(operation);
				break;
			case POST:
				createPostOperation(operation, requestResolvingContext, mapping);
				path.setPost(operation);
				break;
			case PUT:
				createPutOperation(operation, requestResolvingContext, mapping);
				path.setPut(operation);
				break;
			case PATCH:
				createPatchOperation(operation, requestResolvingContext, mapping);
				path.setPatch(operation);
				break;

			default:
				throw new IllegalArgumentException("Method not supported: " + mapping.getMethod());
		}

		// These could be cached? Original comment said "they wont change in most cases"
		List<OpenapiParameter> endpointParameters = getQueryParameterRefs(ApiV1DdraEndpoint.T, endpointParametersResolvingContext, "endpoint");
		operation.getParameters().addAll(endpointParameters);
	}

	private List<EntityType<?>> collectPotentialReasons(EntityType<?> requestType, OpenapiContext context) {
		//@formatter:off
		List<UnsatisfiedBy> unsatisfiedBys = 
				context
				.getMetaData()
				.entityType(requestType)
				.meta(UnsatisfiedBy.T)
				.list();
		return unsatisfiedBys
			.stream()
			.map(UnsatisfiedBy::getReasonType)
			.map(GmEntityType::getTypeSignature)
			.map(EntityTypes::get)
			.collect(Collectors.toList());
		//@formatter:on

	}

	private void createPostOperation(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping) {
		boolean isMultipart = mapping.getAnnounceAsMultipart() == null ? true : mapping.getAnnounceAsMultipart();

		createOperationWithBody(operation, context, mapping, isMultipart);
	}

	private void createPutOperation(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping) {
		boolean isMultipart = mapping.getAnnounceAsMultipart() == null ? false : mapping.getAnnounceAsMultipart();

		createOperationWithBody(operation, context, mapping, isMultipart);
	}

	private void createPatchOperation(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping) {
		boolean isMultipart = mapping.getAnnounceAsMultipart() == null ? false : mapping.getAnnounceAsMultipart();

		createOperationWithBody(operation, context, mapping, isMultipart);
	}

	private void createOperationWithBody(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping, boolean isMultipart) {
		EntityType<?> requestType = mapping.getRequestType();

		OpenapiMimeType[] mimeTypes;

		if (isMultipart) {
			mimeTypes = new OpenapiMimeType[] { MULTIPART_FORMDATA, URLENCODED, APPLICATION_JSON };
		} else {
			mimeTypes = ALL_MIME_TYPES;
		}

		OpenapiRequestBody reqestBody = context.components().requestBody(requestType) //
				.ensure(currentContext -> {
					OpenapiRequestBody b = OpenapiRequestBody.T.create();
					b.setContent(createContent(requestType, currentContext, mimeTypes));
					b.setDescription("Serialized " + requestType.getTypeSignature());
					return b;
				}) //
				.getRef();

		operation.setRequestBody(reqestBody);

		operation.getTags().addAll(mapping.getTags());

	}

	private void createGetOperation(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping) {
		createOperationWithoutBody(operation, context, mapping);
	}

	private void createDeleteOperation(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping) {
		createOperationWithoutBody(operation, context, mapping);
	}

	private void createOperationWithoutBody(OpenapiOperation operation, OpenapiContext context, SingleDdraMapping mapping) {
		EntityType<?> requestType = mapping.getRequestType();

		OpenapiContext urlencodedContext = context.childContext(OpenapiMimeType.URLENCODED);

		List<OpenapiParameter> requestParameters = getQueryParameterRefs(requestType, urlencodedContext, null);

		operation.getParameters().addAll(requestParameters);

		operation.getTags().addAll(mapping.getTags());
	}

	private void createGenericEndpointOperation(HttpRequestMethod method, OpenapiPath path, OpenapiContext context,
			OpenapiContext endpointParametersResolvingContext, EntityType<?> requestType, String pathString, String tag) {

		SingleDdraMappingImpl mapping = createGenericMapping(context, requestType);
		mapping.method = method;
		mapping.pathInfo = pathString;
		mapping.tags = asSet(tag);

		createAnyOperation(context, endpointParametersResolvingContext, mapping, path);
	}

	private SingleDdraMappingImpl createGenericMapping(OpenapiContext context, EntityType<?> requestType) {
		SingleDdraMappingImpl mapping = new SingleDdraMappingImpl();
		mapping.requestType = (EntityType<? extends ServiceRequest>) requestType;

		context.setMapping(mapping);
		return mapping;
	}

	private OpenapiPath createGenericPath(OpenapiContext requestResolvingContext, OpenapiContext endpointParametersResolvingContext, EntityType<?> t,
			String pathString, String tag) {
		OpenapiPath path = OpenapiPath.T.create();
		createGenericEndpointOperation(HttpRequestMethod.GET, path, requestResolvingContext, endpointParametersResolvingContext, t, pathString, tag);
		createGenericEndpointOperation(HttpRequestMethod.POST, path, requestResolvingContext, endpointParametersResolvingContext, t, pathString, tag);

		return path;
	}

	private OpenapiOperation createOperation(OpenapiContext context, GenericModelType responseType, boolean isAuthorizedRequest,
			List<EntityType<?>> potentialReasonTypes) {
		OpenapiOperation operation = OpenapiOperation.T.create();
		addResponsesToOperation(responseType, operation, context, isAuthorizedRequest, potentialReasonTypes);
		return operation;
	}

	@Override
	protected String getTitle(ServiceRequestContext requestContext, OpenapiServicesRequest request) {
		return "Service Requests in " + request.getServiceDomain();
	}

	@Override
	protected ConfiguredModel getConfiguredModel(ServiceRequestContext requestContext, OpenapiServicesRequest request) {
		return requireServiceDomain(request.getServiceDomain()).configuredModel();
	}

	private ServiceDomain requireServiceDomain(String domainId) {
		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		if (serviceDomain == null)
			throw new IllegalArgumentException("Unknown service domain: " + domainId);

		return serviceDomain;
	}

	@Override
	protected String getRelativeEndpointPath(ServiceRequestContext requestContext, OpenapiServicesRequest request) {
		return relativeEndpointPath;
	}

}
