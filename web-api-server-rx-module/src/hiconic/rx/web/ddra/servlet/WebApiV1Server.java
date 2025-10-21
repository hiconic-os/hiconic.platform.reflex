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
package hiconic.rx.web.ddra.servlet;

import static hiconic.rx.web.ddra.endpoints.api.DdraEndpointsUtils.getPathInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.CharsetOption;
import com.braintribe.codec.marshaller.api.EntityVisitorOption;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.gm.model.reason.meta.HttpStatusCode;
import com.braintribe.gm.model.reason.meta.LogReason;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.CollectionType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.logging.LogLevel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.rpc.commons.api.RpcConstants;
import com.braintribe.model.processing.rpc.commons.impl.RpcUnmarshallingStreamManagement;
import com.braintribe.model.processing.service.api.TraversingCriterionAspect;
import com.braintribe.model.processing.service.api.aspect.HttpStatusCodeNotification;
import com.braintribe.model.processing.service.api.aspect.RequestTransportPayloadAspect;
import com.braintribe.model.processing.session.api.managed.NotFoundException;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resource.source.TransientSource;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Unsatisfied;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.collection.api.ListMap;
import com.braintribe.utils.collection.impl.HashListMap;
import com.braintribe.utils.lcd.StringTools;
import com.braintribe.utils.stream.api.StreamPipeFactory;
import com.braintribe.web.multipart.api.FormDataWriter;
import com.braintribe.web.multipart.api.MultipartFormat;
import com.braintribe.web.multipart.api.MutablePartHeader;
import com.braintribe.web.multipart.api.PartReader;
import com.braintribe.web.multipart.api.PartWriter;
import com.braintribe.web.multipart.api.SequentialFormDataReader;
import com.braintribe.web.multipart.impl.MultipartSubFormat;
import com.braintribe.web.multipart.impl.Multiparts;
import com.braintribe.web.multipart.impl.SequentialParallelFormDataWriter;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoder;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;
import dev.hiconic.servlet.decoder.api.StandardHeadersMapper;
import dev.hiconic.servlet.decoder.api.UrlPathCodec;
import dev.hiconic.servlet.decoder.impl.QueryParamDecoder;
import hiconic.rx.module.api.service.PlatformServiceDomains;
import hiconic.rx.web.ddra.endpoints.api.DdraEndpointAspect;
import hiconic.rx.web.ddra.endpoints.api.DdraEndpointsUtils;
import hiconic.rx.web.ddra.endpoints.api.context.HttpRequestSupplier;
import hiconic.rx.web.ddra.endpoints.api.context.HttpRequestSupplierAspect;
import hiconic.rx.web.ddra.endpoints.api.context.HttpResponseConfigurerAspect;
import hiconic.rx.web.ddra.endpoints.api.v1.ApiV1EndpointContext;
import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMapping;
import hiconic.rx.web.ddra.endpoints.api.v1.WebApiMappingOracle;
import hiconic.rx.web.ddra.servlet.ApiV1RestServletUtils.DecodingTargetTraversalResult;
import hiconic.rx.webapi.endpoints.DdraBaseUrlPathParameters;
import hiconic.rx.webapi.endpoints.DdraEndpoint;
import hiconic.rx.webapi.endpoints.DdraEndpointDepth;
import hiconic.rx.webapi.endpoints.DdraEndpointHeaders;
import hiconic.rx.webapi.endpoints.api.v1.ApiV1DdraEndpoint;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The HttpServlet for the /api/v1 pathInfo.
 * <p>
 * This class is thread-safe IF the injected {@code StandardWebApiMappingOracle} is thread-safe.
 */
public class WebApiV1Server extends AbstractDdraRestServlet<ApiV1EndpointContext> {

	private static final String DDRA_MD_USECASE = "ddra";
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(WebApiV1Server.class);

	private final static Set<String> requestAssemblyPartNames = CollectionTools.getSet(RpcConstants.RPC_MAPKEY_REQUEST,
			HttpRequestEntityDecoder.SERIALIZED_REQUEST);

	private static final StandardHeadersMapper<DdraEndpointHeaders> ENDPOINT_MAPPER = StandardHeadersMapper.mapToProperties(DdraEndpointHeaders.T);
	private static final UrlPathCodec<DdraBaseUrlPathParameters> URL_CODEC = UrlPathCodec.<DdraBaseUrlPathParameters> create() //
			.mappedSegment("serviceDomain", true) //
			.mappedSegment("typeSignature");

	private static LogReason defaultLogging;

	static {
		defaultLogging = LogReason.T.create();
		defaultLogging.setLevel(LogLevel.ERROR);
		defaultLogging.setRecursive(true);
	}

	private String defaultServiceDomain = PlatformServiceDomains.main.name();

	private Predicate<String> domainAvailabilityChecker = (domain) -> false;

	private WebApiMappingOracle mappingOralce;

	// TODO review this default service domain
	private StreamPipeFactory streamPipeFactory;
	private Function<String, CmdResolver> mdResolverProvider;

	private ApiV1RestServletUtils restServletUtils;

	@Required
	public void setMdResolverProvider(Function<String, CmdResolver> mdResolverProvider) {
		this.mdResolverProvider = mdResolverProvider;
	}
	
	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void handleGet(ApiV1EndpointContext context) throws IOException {
		handleMethodWithoutBody(context);
	}

	@Override
	protected void handleDelete(ApiV1EndpointContext context) throws IOException {
		handleMethodWithoutBody(context);
	}

	@Override
	protected void handlePost(ApiV1EndpointContext context) throws IOException {
		handleMethodWithBody(context);
	}

	@Override
	protected void handlePut(ApiV1EndpointContext context) throws IOException {
		handleMethodWithBody(context);
	}

	@Override
	protected void handlePatch(ApiV1EndpointContext context) throws IOException {
		handleMethodWithBody(context);
	}

	@Override
	protected void handleOptions(ApiV1EndpointContext context) {
		Collection<String> mappedMethods = mappingOralce.getMethods(getPathInfo(context));

		if (mappedMethods.isEmpty()) {
			decodePathAndFillContext(context);

			if (context.getServiceRequestType() == null) {
				throw new NotFoundException("No implicit or explicit mapping found for '" + getPathInfo(context) + "'.");
			}

			mappedMethods = Arrays.asList("GET", "POST");
		}

		context.getResponse().setStatus(204);
		DdraEndpointsUtils.setAllowHeader(context, mappedMethods);
	}

	@Override
	protected ApiV1EndpointContext createContext(HttpServletRequest request, HttpServletResponse response) {
		ApiV1EndpointContext apiV1EndpointContext = new ApiV1EndpointContext(request, response, defaultServiceDomain);
		apiV1EndpointContext.setMarshaller(marshallerRegistry.getMarshaller("application/json"));
		return apiV1EndpointContext;
	}

	@Override	
	protected boolean fillContext(ApiV1EndpointContext context) {
		if ("OPTIONS".equals(context.getRequest().getMethod())) {
			handleOptions(context);
			return false;
		}

		// compute mapping of current request
		SingleDdraMapping mapping = computeDdraMapping(context);

		// get the entity type for the pathInfo or mapping
		if (mapping != null) {
			context.setServiceDomain(mapping.getServiceDomain());

		} else {
			Collection<String> allowedHttpMethodsForMapping = mappingOralce.getMethods(getPathInfo(context));

			if (!allowedHttpMethodsForMapping.isEmpty()) {
				// the mapping is available under a different method. Send 405
				context.getResponse().setStatus(405);
				DdraEndpointsUtils.setAllowHeader(context, allowedHttpMethodsForMapping);
				commitResponse(context);
				return false;
			}

			decodePathAndFillContext(context);
		}

		// get the out marshaller as early as possible to write exceptions with proper mimeType.
		ApiV1DdraEndpoint endpoint = restServletUtils.createDefaultEndpoint(mapping);
		context.setEndpoint(endpoint);

		return true;
	}

	private void commitResponse(ApiV1EndpointContext context) {
		try {
			// commit response so that nothing can be accidentally added afterwards
			context.getResponse().flushBuffer();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void handleMethodWithoutBody(ApiV1EndpointContext context) throws IOException {
		EntityType<? extends ServiceRequest> serviceRequestType = context.getServiceRequestType();

		ServiceRequest service = null;
		if (serviceRequestType != null) {
			service = createDefaultRequest(serviceRequestType);
			decodeQueryAndFillContext(service, context);
		} else {
			writeUnsatisfied(context, Reasons.build(InvalidArgument.T).text("Missing service request type").toMaybe(), 400);
			return;
		}

		// TODO
		// if (ResourceDownloadHandler.handleRequest(context, service, userSessionFactory, modelAccessoryFactory))
		// return;
		// if (ResourceDeleteHandler.handleRequest(context, service, userSessionFactory, modelAccessoryFactory))
		// return;

		// get the transform request (from the mapping) if any
		service = restServletUtils.computeTransformRequest(context, service);

		// compute the output marshaller
		computeOutMarshallerFor(context, context.getDefaultMimeType());

		processRequestAndWriteResponse(context, service);
	}

	private void handleMethodWithBody(ApiV1EndpointContext context) throws IOException {
		EntityType<? extends ServiceRequest> serviceRequestType = context.getServiceRequestType();

		decodeQueryAndFillContext(null, context);

		// compute output marshaller
		computeOutMarshallerFor(context, context.getDefaultMimeType());

		// TODO: remove
		// if (serviceRequestType != null
		// && ResourceUploadHandler.handleRequest(context, marshallerRegistry, serviceRequestType.create(), userSessionFactory))
		// {
		// return;
		// }

		RpcUnmarshallingStreamManagement streamManagement = new RpcUnmarshallingStreamManagement("rest-api-v1", streamPipeFactory);
		context.setRequestStreamManagement(streamManagement);

		HttpServletRequest request = context.getRequest();

		GmDeserializationOptions options = GmDeserializationOptions.defaultOptions.derive() //
				.setInferredRootType(serviceRequestType) //
				.set(EntityVisitorOption.class, streamManagement.getMarshallingVisitor()) //
				.set(CharsetOption.class, request.getCharacterEncoding()) //
				.build();

		SingleDdraMapping mapping = context.getMapping();

		ApiV1DdraEndpoint endpoint = context.getEndpoint();

		ServiceRequest service = null;
		MultipartFormat requestMultipartFormat = context.getRequestMultipartFormat();
		
		InputStream requestIn = request.getInputStream();
		
		if (requestMultipartFormat.getSubFormat() == MultipartSubFormat.formData) {
			String boundary = requestMultipartFormat.getParameter("boundary");

			if (boundary == null) {
				throw new IllegalArgumentException(
						"Illegal Request: Content-Type was 'multipart/form-data' but without the mandatory 'boundary' parameter.");
			}

			Maybe<ServiceRequest> serviceMaybe = parseMultipartRequest(boundary, context);
			
			if (serviceMaybe.isUnsatisfied()) {
				writeUnsatisfied(context, serviceMaybe, 400);
				return;
			}
			
			service = serviceMaybe.get();

		} else {
			if ("application/x-www-form-urlencoded".equals(endpoint.getContentType())) {
				ListMap<String, String> parameters = new HashListMap<>();
				request.getParameterMap().forEach((k, v) -> parameters.put(k, Arrays.asList(v)));

				UrlEncodingMarshaller.EntityTemplateFactory rootEntityFactory = l -> requestAssemblyPartNames.stream() //
						.map(l::getSingleElement) //
						.filter(Objects::nonNull) //
						.findFirst() //
						.map(a -> (GenericEntity) jsonMarshaller.unmarshall(new StringReader(a), options)) //
						.orElseGet(() -> createDefaultRequest(serviceRequestType));

				UrlEncodingMarshaller urlMarshaller = new UrlEncodingMarshaller(rootEntityFactory);
				service = urlMarshaller.create(parameters, serviceRequestType, options);

			} else {
				Marshaller inMarshaller = getInMarshallerFor(endpoint);

				boolean transportPayload = false;
				if (mapping != null) {
					transportPayload = Boolean.TRUE.equals(mapping.getDefaultPreserveTransportPayload());
				}

				// TODO: think about transport payload support. A TeeInputStream along with a StreamPipe was used to achieve that
				if (transportPayload) {
					logger.warn("DdraMapping.defaultPreserveTransportPayload is currently not supported and ignored");
				}
				try (InputStream in = requestIn) {
					// Unmarshall the request from the body
					Maybe<?> maybeService = inMarshaller.unmarshallReasoned(in, options);
					
					if (maybeService.isUnsatisfied()) {
						Maybe<Object> maybe = Reasons.build(InvalidArgument.T).text("Invalid HTTP request body").cause(maybeService.whyUnsatisfied()).toMaybe();
						Integer statusCode = maybeService.isUnsatisfiedBy(ParseError.T)? 400: 500;
						
						writeUnsatisfied(context, maybe, statusCode);
						return;
					}
					
					service = (ServiceRequest) maybeService.get();
				}
			}
		}

		if (service == null)
			// If no body is provided at all the unmarshaller returns null. We supply a default in that case.
			service = createDefaultRequest(serviceRequestType);

		decodeQueryAndFillContext(service, context);

		restServletUtils.ensureServiceDomain(service, context);
		// get the transform request (from the mapping) if any
		service = restServletUtils.computeTransformRequest(context, service);

		GenericModelType evaluatesTo = service.entityType().getEffectiveEvaluatesTo();
		if (evaluatesTo == null)
			context.setExpectedResponseType(BaseType.INSTANCE);
		else
			context.setExpectedResponseType(evaluatesTo);

		processRequestAndWriteResponse(context, service);
	}

	private ServiceRequest createDefaultRequest(EntityType<? extends ServiceRequest> serviceRequestType) {
		return serviceRequestType.create();
	}

	private Maybe<ServiceRequest> parseMultipartRequest(String boundary, ApiV1EndpointContext context) {
		final ServiceRequest service;
		RpcUnmarshallingStreamManagement streamManagement = context.getRequestStreamManagement();
		EntityType<? extends ServiceRequest> serviceRequestType = context.getServiceRequestType();

		try (SequentialFormDataReader formDataReader = Multiparts.formDataReader(context.getRequest().getInputStream(), boundary).autoCloseInput()
				.sequential()) {
			PartReader part = formDataReader.next();

			if (part != null && requestAssemblyPartNames.contains(part.getName())) {
				try (InputStream in = part.openStream()) {
					// get input marshaller
					Marshaller marshaller = getInMarshallerFor(part.getContentType());
					// Unmarshall the request from the body
					GmDeserializationOptions options = GmDeserializationOptions.defaultOptions.derive() //
							.setInferredRootType(serviceRequestType) //
							.set(EntityVisitorOption.class, streamManagement.getMarshallingVisitor()) //
							.build();

					Maybe<?> serviceMaybe = marshaller.unmarshallReasoned(in, options);
					
					if (serviceMaybe.isUnsatisfied()) {
						return serviceMaybe.whyUnsatisfied().asMaybe();
					}
					
					service = (ServiceRequest) serviceMaybe.get();
				}

				part = formDataReader.next();
			} else {
				service = createDefaultRequest(serviceRequestType);
			}

			restServletUtils.ensureServiceDomain(service, context);

			ModelMdResolver mdResolver = mdResolverProvider.apply(context.getServiceDomain()) //
					.getMetaData() //
					.useCases(DDRA_MD_USECASE);

			HttpRequestEntityDecoderOptions options = HttpRequestEntityDecoderOptions.defaults();
			QueryParamDecoder decoder = new QueryParamDecoder(options);
			decoder.registerTarget("service", service);

			Map<String, List<Resource>> resourceLists = new HashMap<>();
			Map<String, Set<Resource>> resourceSets = new HashMap<>();
			Map<String, DecodingTargetTraversalResult> resources = new HashMap<>();

			List<DecodingTargetTraversalResult> traversalResults = restServletUtils.traverseDecodingTarget(service, decoder, mdResolver);
			traversalResults.stream() //
					.forEach(r -> {
						GenericModelType propertyType = r.getProperty().getType();
						if (propertyType == Resource.T) {
							resources.put(r.prefixedPropertyName(), r);
						} else if (propertyType.isCollection() && ((CollectionType) propertyType).getCollectionElementType() == Resource.T) {
							Object ownValue = r.getOwnValue();
							if (ownValue instanceof List) {
								resourceLists.put(r.prefixedPropertyName(), (List<Resource>) ownValue);
							} else if (ownValue instanceof Set) {
								resourceSets.put(r.prefixedPropertyName(), (Set<Resource>) ownValue);
							}
						}
					});

			while (part != null) {
				String partName = part.getName();

				if (requestAssemblyPartNames.contains(partName)) {
					return Reasons.build(InvalidArgument.T).text("Duplicate request assembly part in multipart message: " + part).toMaybe();
				}

				TransientSource transientSourceWithId = streamManagement.getTransientSourceWithId(partName);
				if (transientSourceWithId != null) {
					restServletUtils.processResourcePart(streamManagement, part, transientSourceWithId);
				} else if (resourceLists.containsKey(partName)) {
					Resource resource = createEmptyTransientResource();
					resourceLists.get(partName).add(resource);
					restServletUtils.processResourcePart(streamManagement, part, (TransientSource) resource.getResourceSource());
				} else if (resourceSets.containsKey(partName)) {
					Resource resource = createEmptyTransientResource();
					resourceSets.get(partName).add(resource);
					restServletUtils.processResourcePart(streamManagement, part, (TransientSource) resource.getResourceSource());
				} else if (resources.containsKey(partName)) {
					DecodingTargetTraversalResult decodingTargetTraversalResult = resources.get(partName);
					Resource resource = (Resource) decodingTargetTraversalResult.ensureOwnEntity();
					ResourceSource resourceSource = resource.getResourceSource();
					if (resourceSource == null) {
						resourceSource = createEmptyTransientSource(resource);
					} else if (!(resourceSource instanceof TransientSource)) {
						throw new IllegalArgumentException("Error while handling part '" + partName
								+ "'. Can't assign binary data to a resource that has already a non-transient ResourceSource." + resource);
					}

					restServletUtils.processResourcePart(streamManagement, part, (TransientSource) resourceSource);
				} else {
					decoder.decode(partName, part.getContentAsString());
				}

				part = formDataReader.next();
			}

		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Error while reading multiparts");
		}

		streamManagement.checkPipeSatisfaction();
		return Maybe.complete(service);
	}

	private Resource createEmptyTransientResource() {
		Resource resource = Resource.T.create();
		resource.setResourceSource(createEmptyTransientSource(resource));
		return resource;
	}

	private TransientSource createEmptyTransientSource(Resource resource) {
		TransientSource result = TransientSource.T.create();
		result.setGlobalId(UUID.randomUUID().toString());
		result.setOwner(resource);
		resource.setResourceSource(result);
		return result;
	}
	
	private void processRequestAndWriteResponse(ApiV1EndpointContext context, ServiceRequest service) throws IOException {

		ApiV1DdraEndpoint endpoint = context.getEndpoint();

		if (!context.isMultipartResponse()) {

			getStandardResponse(context, endpoint, service);
		} else {
			getMultipartResponse(context, endpoint, service);
		}
	}

	private void getMultipartResponse(ApiV1EndpointContext context, ApiV1DdraEndpoint endpoint, ServiceRequest service) throws IOException {
		RpcUnmarshallingStreamManagement requestStreamManagement = context.getRequestStreamManagement();
		String boundary = Multiparts.generateBoundary();
		context.setMultipartResponseContentType(boundary);
		HttpServletResponse httpResponse = context.getResponse();
		FormDataWriter blobFormDataWriter = Multiparts.blobFormDataWriter(httpResponse.getOutputStream(), boundary);

		try (FormDataWriter formDataWriter = new SequentialParallelFormDataWriter(blobFormDataWriter, streamPipeFactory)) {
			context.setResponseOutputStreamProvider(() -> {
				MutablePartHeader header = Multiparts.newPartHeader();
				header.setName(RpcConstants.RPC_MAPKEY_RESPONSE);
				header.setContentType(context.getMimeType());
				PartWriter part = formDataWriter.openPart(header);

				return part.outputStream();
			});
			if (requestStreamManagement != null) {
				for (CallStreamCapture capture : requestStreamManagement.getCallStreamCaptures()) {
					capture.setOutputStreamProvider(() -> {
						MutablePartHeader header = Multiparts.newPartHeader();
						header.setName(capture.getGlobalId());
						header.setContentType("application/octet-stream");
						PartWriter part = formDataWriter.openPart(header);

						return part.outputStream();
					});
				}
			}
			Maybe<?> maybe = evaluateServiceRequest(service, context);

			if (maybe.isSatisfied()) {
				Object response = maybe.get();
				Object projectedResponse = restServletUtils.project(context, endpoint, response);
				writeResponse(context, projectedResponse, endpoint, false);
			} else {
				writeUnsatisfied(context, maybe);
			}

			// writing transient resources
			restServletUtils.writeOutTransientSources(context, formDataWriter);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not prepare writing multipart response.");
		}
	}

	private void getStandardResponse(ApiV1EndpointContext context, ApiV1DdraEndpoint endpoint, ServiceRequest service) throws IOException {
		RpcUnmarshallingStreamManagement requestStreamManagement = context.getRequestStreamManagement();

		if (requestStreamManagement != null) {
			requestStreamManagement.disarmCallStreamCaptures();
		}

		String responseContentType = context.getEndpoint().getResponseContentType();
		if (responseContentType != null) {
			context.setMimeType(responseContentType);
		}

		Maybe<?> maybe = evaluateServiceRequest(service, context);

		if (maybe.isSatisfied()) {
			Object response = maybe.get();
			Object projectedResponse = restServletUtils.project(context, endpoint, response);

			if (context.isResourceDownloadResponse()) {
				handleResourceDownloadResponse(context, projectedResponse, responseContentType);
			} else {
				context.ensureContentDispositionHeader(null);
				writeResponse(context, projectedResponse, endpoint, false);
			}
		} else {
			writeUnsatisfied(context, maybe);
		}
	}

	private void writeUnsatisfied(ApiV1EndpointContext context, Maybe<?> maybe) throws IOException {
		writeUnsatisfied(context, maybe, null);
	}
	
	private void writeUnsatisfied(ApiV1EndpointContext context, Maybe<?> maybe, Integer httpStatusCode) throws IOException {
		Reason reason = maybe.whyUnsatisfied();

		String domainId = context.getServiceDomain();

		EntityMdResolver reasonMdResolver = mdResolverProvider.apply(domainId).getMetaData().lenient(true).entity(reason).useCase(DDRA_MD_USECASE);

		// logging
		LogReason logReason = Optional.ofNullable(reasonMdResolver.meta(LogReason.T).exclusive()).orElse(defaultLogging);

		LogLevel logLevel = logReason.getLevel();

		if (logLevel != null) {
			String msg = logReason.getRecursive() ? reason.stringify() : reason.asString();
			if (reason instanceof InternalError) {
				InternalError ie = (InternalError) reason;
				logger.log(translateLogLevel(logLevel), msg, ie.getJavaException());
			} else {
				logger.log(translateLogLevel(logLevel), msg);
			}
		}

		// status code handling
		if (!context.getResponse().isCommitted()) {
			
			if (httpStatusCode == null) {
				Optional<HttpStatusCode> statusOptional = Optional.ofNullable(reasonMdResolver.meta(HttpStatusCode.T).exclusive());
				httpStatusCode = statusOptional.map(HttpStatusCode::getCode).orElse(500);
			}

			context.getResponse().setStatus(httpStatusCode);
		}

		// marshaling reason
		writeResponse(context, Unsatisfied.from(maybe), Unsatisfied.T, true);
	}

	private com.braintribe.logging.Logger.LogLevel translateLogLevel(LogLevel logLevel) {
		try {
			return com.braintribe.logging.Logger.LogLevel.valueOf(logLevel.name());
		} catch (Exception e) {
			logger.warn("Unsupported log level " + logLevel + ". Falling back to level ERROR.");
			return com.braintribe.logging.Logger.LogLevel.ERROR;
		}
	}

	protected Maybe<?> evaluateServiceRequest(ServiceRequest service, ApiV1EndpointContext context) {
		DdraEndpoint endpoint = context.getEndpoint();
		DdraEndpointDepth computedDepth = endpoint.getComputedDepth();
		TraversingCriterion criterion = (computedDepth != null) ? this.traversingCriteriaMap.getCriterion(computedDepth) : null;

		HttpRequestSupplier httpRequestSupplier = new HttpRequestSupplierImpl(service, context.getRequest());
		HttpResponseConfigurerImpl httpResponseConfigurer = new HttpResponseConfigurerImpl();

		//@formatter:off
		EvalContext<Object> evalContext = evaluator
			.eval(service)
			.with(DdraEndpointAspect.class, endpoint)
			.with(TraversingCriterionAspect.class, criterion)
			.with(HttpStatusCodeNotification.class, context::setForceResponseCode)
			.with(HttpRequestSupplierAspect.class, httpRequestSupplier)
			.with(HttpResponseConfigurerAspect.class, httpResponseConfigurer);
		//@formatter:on

		if (context.getRequestTransportPayload() != null) {
			evalContext.setAttribute(RequestTransportPayloadAspect.class, context.getRequestTransportPayload());
		}
		Maybe<?> maybe = evalContext.getReasoned();

		if (maybe.isSatisfied()) {
			Object result = maybe.get();
			httpResponseConfigurer.consume(result, context.getResponse());
		}

		return maybe;
	}

	private void handleResourceDownloadResponse(ApiV1EndpointContext context, Object projectedResponse, String responseContentType)
			throws IOException {

		if (projectedResponse instanceof Resource) {
			Resource resource = (Resource) projectedResponse;
			context.ensureContentDispositionHeader(resource.getName());

			if (resource.getFileSize() != null)
				context.getResponse().setContentLength(resource.getFileSize().intValue());

			if (responseContentType == null) {
				String mimeType = resource.getMimeType();
				context.setMimeType(mimeType);
			}

			try (OutputStream responseOut = context.openResponseOutputStream()) {
				resource.writeToStream(responseOut);
			}
		} else {
			try (OutputStream responseOut = context.openResponseOutputStream()) {
				// Just open and close the stream, assuming that this means sending an empty response
			}
		}
	}

	private SingleDdraMapping computeDdraMapping(ApiV1EndpointContext context) {
		HttpRequestMethod method = HttpRequestMethod.valueOf(context.getRequest().getMethod().toUpperCase());
		String path = getPathInfo(context);
		SingleDdraMapping mapping = mappingOralce.get(path, method);

		context.setMapping(mapping);
		return mapping;
	}

	// Only called when no mapping is found 
	private EntityType<? extends ServiceRequest> decodePathAndFillContext(ApiV1EndpointContext context) {
		// No mapping found. Identify type and domain from Path
		DdraBaseUrlPathParameters pathParameters = DdraBaseUrlPathParameters.T.create();
		URL_CODEC.decode(() -> pathParameters, getPathInfo(context));

		String serviceDomain = pathParameters.getServiceDomain();
		if (serviceDomain == null) {
			String typeSignature = pathParameters.getTypeSignature();
			if (!StringTools.isEmpty(typeSignature)) {
				serviceDomain = typeSignature;
				pathParameters.setServiceDomain(serviceDomain);
				pathParameters.setTypeSignature(null);
			} else {
				serviceDomain = defaultServiceDomain;
				pathParameters.setServiceDomain(serviceDomain);
				pathParameters.setIsDomainExplicit(false);
			}
		}

		context.setServiceDomain(serviceDomain);
		checkServiceDomain(context);

		// get the type signature from the pathInfo
		String typeSignature = pathParameters.getTypeSignature();
		if (StringUtils.isBlank(typeSignature)) {
			return null;
		}
		
		// get the entity type from the type signature
		ModelOracle modelOracle = mdResolverProvider.apply(serviceDomain).getModelOracle();
		EntityType<? extends ServiceRequest> entityType = restServletUtils.resolveTypeFromSignature(typeSignature, modelOracle);
		if (entityType == null)
			HttpExceptions.throwNotFound("Cannot find request [%s]", typeSignature);

		if (!ServiceRequest.T.isAssignableFrom(entityType))
			HttpExceptions.throwBadRequest("Entity [%s] is not a ServiceRequest.", typeSignature);

		context.setServiceRequestType(entityType);
		return entityType;
	}

	private void decodeQueryAndFillContext(ServiceRequest service, ApiV1EndpointContext context) {
		HttpRequestEntityDecoderOptions options = HttpRequestEntityDecoderOptions.defaults();
		requestAssemblyPartNames.forEach(options::addIgnoredParameter);

		HttpRequestEntityDecoder decoder = HttpRequestEntityDecoder.createFor(context.getRequest(), options);
		if (service != null) {
			decoder.target("service", service); // TODO: remove or rename if possible
			// decoder.target("", service);

			String serviceDomain = context.getServiceDomain();

			ModelMdResolver metaDataResolver = mdResolverProvider.apply(serviceDomain) //
					.getMetaData() //
					.useCase(DDRA_MD_USECASE);

			if (context.getMapping() != null && context.getMapping().getPathInfo() != null) {
				String mappingSpecificUsecase = DDRA_MD_USECASE + ":" + context.getMapping().getPathInfo();
				metaDataResolver.useCase(mappingSpecificUsecase);
			}

			restServletUtils.traverseDecodingTarget(service, decoder, metaDataResolver);
			restServletUtils.ensureServiceDomain(service, context);
		} else {
			options.setIgnoringUnmappedHeaders(true);
			options.setIgnoringUnmappedUrlParameters(true);
		}

		DdraEndpoint endpoint = context.getEndpoint();

		decoder.target("endpoint", endpoint, ENDPOINT_MAPPER).decode();

		DdraEndpointsUtils.computeDepth(endpoint);

	}

	private void checkServiceDomain(ApiV1EndpointContext context) {
		String serviceDomain = context.getServiceDomain();
		if (!domainAvailabilityChecker.test(serviceDomain))
			HttpExceptions.throwNotFound(
					"No ServiceDomain or DdraMapping found for name: " + serviceDomain + " and HTTP method: " + context.getRequest().getMethod());
	}

	@Required
	public void setMappingOralce(WebApiMappingOracle mappingOralce) {
		this.mappingOralce = mappingOralce;
	}

	@Required
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = streamPipeFactory;
	}

	@Required
	public void setRestServletUtils(ApiV1RestServletUtils restServletUtils) {
		this.restServletUtils = restServletUtils;
	}


	@Required
	public void setDomainAvailability(Predicate<String> domainAvailabilityChecker) {
		this.domainAvailabilityChecker = domainAvailabilityChecker;
	}

	@Configurable
	public void setDefaultServiceDomain(String defaultServiceDomain) {
		this.defaultServiceDomain = defaultServiceDomain;
	}

}
