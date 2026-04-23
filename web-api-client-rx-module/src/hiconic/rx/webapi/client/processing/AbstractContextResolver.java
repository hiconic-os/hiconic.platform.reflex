// ============================================================================
package hiconic.rx.webapi.client.processing;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.codec.marshaller.api.PropertyTypeInferenceOverride;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.CollectionType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.GenericModelTypeReflection;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.model.meta.GmType;
import com.braintribe.model.meta.data.EntityTypeMetaData;
import com.braintribe.model.meta.data.constraint.TypeSpecification;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.PropertyMdResolver;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.HttpConstants;
import hiconic.rx.webapi.client.api.HttpParameter;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;
import hiconic.rx.webapi.client.model.meta.HttpConsumes;
import hiconic.rx.webapi.client.model.meta.HttpDateFormatting;
import hiconic.rx.webapi.client.model.meta.HttpDefaultFailureResponseType;
import hiconic.rx.webapi.client.model.meta.HttpDefaultSuccessResponseType;
import hiconic.rx.webapi.client.model.meta.HttpMethod;
import hiconic.rx.webapi.client.model.meta.HttpParam;
import hiconic.rx.webapi.client.model.meta.HttpParamType;
import hiconic.rx.webapi.client.model.meta.HttpPath;
import hiconic.rx.webapi.client.model.meta.HttpProduces;
import hiconic.rx.webapi.client.model.meta.HttpSuccessCodes;
import hiconic.rx.webapi.client.model.meta.params.HttpBodyParam;
import hiconic.rx.webapi.client.model.meta.params.HttpRequestIsBody;
import hiconic.rx.webapi.client.model.meta.params.HttpResourceStreamBodyParam;

public abstract class AbstractContextResolver implements HttpContextResolver {

	private static final Logger logger = Logger.getLogger(AbstractContextResolver.class);

	private static final GenericModelTypeReflection typeReflection = GMF.getTypeReflection();

	protected Set<String> resolverUseCases = Collections.singleton("remote-http");

	private int defaultSuccessCode = HttpConstants.HTTP_CODE_OK;

	private static final GenericModelType bodyParametersType = GMF.getTypeReflection().getType("map<string,object>");

	@Configurable
	public void setResolverUseCases(Set<String> resolverUseCases) {
		this.resolverUseCases = resolverUseCases;
	}

	@Configurable
	public void setDefaultSuccessCode(int defaultSuccessCode) {
		this.defaultSuccessCode = defaultSuccessCode;
	}

	abstract protected ModelMdResolver getModelResolver(ServiceRequestContext serviceContext, ServiceRequest serviceRequest);
	abstract protected HttpClient getHttpClient(RequestContextResolver contextResolver);

	// ***************************************************************************************************
	// ContextResolver
	// ***************************************************************************************************

	@Override
	public HttpRequestContext resolve(ServiceRequestContext serviceContext, ServiceRequest serviceRequest) {

		ModelMdResolver modelResolver = getModelResolver(serviceContext, serviceRequest);

		RequestContextResolver resolver = new RequestContextResolver(serviceContext, serviceRequest, modelResolver);
		HttpClient httpClient = getHttpClient(resolver);

		HttpRequestContextBuilder contextBuilder = HttpRequestContextBuilder.instance(httpClient);
		resolver.resolveParameters();
		resolver.resolveResponseTypes(contextBuilder);

		resolver.resolve(resolver::getQueryParameters, contextBuilder::addQueryParameters);
		resolver.resolve(resolver::getHeaderParameters, contextBuilder::addHeaderParameters);
		resolver.resolve(resolver::resolveRequestMethod, contextBuilder::requestMethod);
		resolver.resolve(resolver::resolveRequestIsBody, md -> {
			if (md != null) {
				contextBuilder.payload(serviceRequest);
			}
		});
		resolver.resolve(resolver::resolveRequestPath, contextBuilder::requestPath);
		resolver.resolve(resolver::resolveProduces, contextBuilder::produces);
		resolver.resolve(resolver::resolveConsumes, contextBuilder::consumes);
		resolver.resolve(resolver::resolvePayload, p -> {
			if (p == resolver.bodyParameters) {
				contextBuilder.payloadType(bodyParametersType);
			}
			contextBuilder.streamResourceContent(resolver.streamResourceContent);
			contextBuilder.payloadIfEmpty(p);
		});

		HttpDateFormatting dateFormatting = resolver.resolveDateFormatting();
		if (dateFormatting != null) {
			contextBuilder.dateFormatting(dateFormatting.getDateFormat(), dateFormatting.getDefaultZone(), dateFormatting.getDefaultLocale());
		}

		BiFunction<EntityType<?>, Property, GenericModelType> inferer = serviceContext.findOrNull(PropertyTypeInferenceOverride.class);

		contextBuilder.propertyTypeInference((entityType, property) -> {
			if (inferer != null) {
				GenericModelType propertyType = inferer.apply(entityType, property);
				if (propertyType != null)
					return propertyType;
			}

			//@formatter:off
			TypeSpecification inferredType = 
					modelResolver
						.entityType(entityType)
						.property(property)
						.meta(TypeSpecification.T)
						.exclusive();
			//@formatter:off
			return (inferredType != null) ? typeReflection.getType(inferredType.getType().getTypeSignature()) : property.getType();
		});
		
		// Compute
		GenericModelType evaluatesTo = resolver.requestType.getEffectiveEvaluatesTo();
		if (evaluatesTo != null) {
			if (evaluatesTo.isEntity()) {
				EntityType<?> responseEntityType = (EntityType<?>) evaluatesTo;
				EntityMdResolver mdResolver = modelResolver.entityType(responseEntityType);
				Property bodyStreamingProperty = responseEntityType.getProperties().stream().filter(p -> Resource.T.isAssignableFrom(p.getType()))
				.filter(rp -> {
					HttpResourceStreamBodyParam md = mdResolver.property(rp).meta(HttpResourceStreamBodyParam.T).exclusive();
					return md != null;
				}).findFirst().orElse(null);
				if (bodyStreamingProperty != null) {
					contextBuilder.streamContentResponseResourceProperty(bodyStreamingProperty.getName());
				}
			}
		}
		
		contextBuilder.responseBodyParameterTranslation((entityType, parameterName) -> {
			PropertyTranslation propertyTranslation = resolver.responsePropertyTranslations.get(entityType);
			Property property = null;
			if (propertyTranslation != null) {
				property = propertyTranslation.bodyParameters.get(parameterName);
			}
			return property != null ? property : entityType.findProperty(parameterName); 
		});
		
		contextBuilder.requestBodyParameterTranslation(property -> {
			//@formatter:off
			HttpParam md = 
					modelResolver
						.property(property)
						.meta(HttpParam.T)
						.exclusive();
			//@formatter:off
			return resolver.resolveParameterName(property, md);
		});
		
		return contextBuilder.build();
	}
	
	// ***************************************************************************************************
	// Helper
	// ***************************************************************************************************
	
	protected class RequestContextResolver {
		
		protected ServiceRequestContext serviceContext;
		protected ServiceRequest serviceRequest;
		protected ModelMdResolver modelResolver;
		protected EntityMdResolver entityResolver;
		protected EntityType<GenericEntity> requestType;
		
		protected Map<String, Object> pathParameters = new HashMap<>();
		protected Map<String, Object> bodyParameters = new HashMap<>();
		
		protected Map<EntityType<?>, PropertyTranslation> responsePropertyTranslations = new HashMap<>();
		
		protected List<HttpParameter> headerParameters = new ArrayList<>();
		protected List<HttpParameter> queryParameters = new ArrayList<>();
		
		protected boolean streamResourceContent = false;
		
		public RequestContextResolver(ServiceRequestContext serviceContext, ServiceRequest serviceRequest, ModelMdResolver modelResolver) {
			this.serviceContext = serviceContext;
			this.serviceRequest = serviceRequest;
			this.modelResolver = modelResolver;
			this.requestType = this.serviceRequest.entityType();
			this.entityResolver = this.modelResolver.entity(this.serviceRequest);
		}
		
		public List<HttpParameter> getHeaderParameters() {
			return headerParameters;
		}
		
		public List<HttpParameter> getQueryParameters() {
			return queryParameters;
		}
		
		private String resolveRequestPath() {
			String pathTemplate = resolveMd(HttpPath.T, HttpPath::getPath);
			return (pathTemplate != null) ? resolveTemplate(pathTemplate) : null;
		}

		private String resolveTemplate(String pathTemplate) {
			return StringTools.patternFormat(pathTemplate, this.pathParameters);
		}

		private String resolveRequestMethod() {
			return resolveMd(HttpMethod.T, this::resolveMethod);
		}
		
		private Object resolveRequestIsBody() {
			return resolveMd(HttpRequestIsBody.T, md -> {return md;});
		}
		
		private String resolveProduces() {
			return resolveMd(HttpProduces.T, HttpProduces::getMimeType);
		}

		private String resolveConsumes() {
			return resolveMd(HttpConsumes.T, HttpConsumes::getMimeType);
		}
		
		private HttpDateFormatting resolveDateFormatting() {
			return this.entityResolver.meta(hiconic.rx.webapi.client.model.meta.HttpDateFormatting.T).exclusive();
		}

		private Object resolvePayload() {
			switch (bodyParameters.size()) {
			case 0: return null;
			case 1: return bodyParameters.values().iterator().next();
			default: 
				return this.bodyParameters;
			}
		}

		private void resolveParameters() {
			this.serviceRequest.entityType().getProperties()
				.stream()
				.forEach(this::resolveParameter);
		}

		private void resolveParameter(Property p) {
			PropertyMdResolver propertyResolver = this.entityResolver.property(p);
			HttpParam restParam = propertyResolver.meta(HttpParam.T).exclusive();
			
			if (restParam != null) {
				switch (restParam.paramType()) {
				case HEADER:
					resolveParameter(p, restParam, this.headerParameters::add);
					break;
				case QUERY:
					resolveParameter(p, restParam, this.queryParameters::add);
					break;
				case BODY:
					// no value resolving for body parameters. thats the marshallers job.
						Object value = p.get(this.serviceRequest);
						HttpBodyParam bodyParam = (HttpBodyParam) restParam;
						if (value != null || !bodyParam.getIgnoreEmptyValue()) {
							this.bodyParameters.put(resolveParameterName(p, restParam), value); 
						}
						if (bodyParam instanceof HttpResourceStreamBodyParam) {
							streamResourceContent = true;
						}
					break;
				case PATH:
					this.pathParameters.put(resolveParameterName(p, restParam), resolveParameterValue(p, restParam));
					break;
				case UNMAPPED:
					// the property is configured to be unmapped, so we simply ignore it.
					break;
				}
			} else {
				// We add all properties as path parameter if not done already with specific md configuration
				this.pathParameters.put(resolveParameterName(p, null), resolveParameterValue(p, null));
			}
		}

		private void resolveParameter(Property p, HttpParam restParam, Consumer<HttpParameter> consumer) {
			String parameterName = resolveParameterName(p, restParam);
			
			resolveParameterValues(p, restParam)
				.stream()
				.map(v -> new HttpParameter(parameterName, v))
				.forEach(consumer);
			
		}

		private String resolveParameterName(Property p, HttpParam restParam) {
			String paramName = null;
			if (restParam != null) {
				paramName = restParam.getParamName();
			}
			return (paramName != null) ? paramName : p.getName();
		}
		
		private String resolveParameterValue(Property p, HttpParam restParam) {
			Collection<String> values = resolveParameterValues(p, restParam);
			return (values.size() > 0) ? values.iterator().next() : null;
		}
		
		private Collection<String> resolveParameterValues(Property p, @SuppressWarnings("unused") HttpParam restParam) {
			Object value = p.get(this.serviceRequest);
			return (value != null) ? encodeValues(value) : Collections.emptyList(); // TODO: handle encoding
		}
		
		@SuppressWarnings("fallthrough")
		private List<String> encodeValues(Object value) {
			if (value == null) {
				return Collections.emptyList();
			}
			
			List<String> encodedValues = new ArrayList<>();
			
			GenericModelType type = typeReflection.getType(value);
			switch (type.getTypeCode()) {
			case booleanType:
			case decimalType:
			case doubleType:
			case stringType:
			case floatType:
			case integerType:
			case longType:
			case enumType: {
				ScalarType scalarType = (ScalarType) type;
				String encodedValue = scalarType.instanceToString(value);
				encodedValues.add(encodedValue);
				break;
			}
			case dateType: {
				HttpDateFormatting dateFormatting = resolveDateFormatting();
				if (dateFormatting != null) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatting.getDateFormat());
					String encodedValue = DateTools.encode((Date) value, formatter);
					encodedValues.add(encodedValue);
				} else {
					ScalarType scalarType = (ScalarType) type;
					String encodedValue = scalarType.instanceToString(value);
					encodedValues.add(encodedValue);
				}
				break;
			}			case setType:
			case listType:
				CollectionType collectionType = (CollectionType) type;
				GenericModelType elementType = collectionType.getCollectionElementType();
				if (elementType.isScalar()) {
					@SuppressWarnings("unchecked")
					Collection<Object> values = (Collection<Object>) value;
					values.stream()
					.map(this::encodeValues)
					.forEach(encodedValues::addAll);
					break;
				}
			default:
				//TODO: we currently do not support anything else then scalar types or collections of scalar. 
				// throw new HttpProcessingException("Unsupported parameter type: "+type.getTypeSignature());
				break;
			}

			return encodedValues;
		}

		
		private void resolveResponseTypes(HttpRequestContextBuilder contextBuilder) {

			GenericModelType evaluatesTo = this.requestType.getEffectiveEvaluatesTo();
			if (evaluatesTo != null) {
				indexPotentialResponseType(evaluatesTo);
			} else {
				logger.warn(() -> "The response type of "+this.requestType+" is null.");
			}
			
			HttpSuccessCodes successCodes = this.entityResolver.meta(HttpSuccessCodes.T).exclusive();
			if (successCodes != null) {
				successCodes.getSuccessCodes().forEach(c -> contextBuilder.addResponseType(c, evaluatesTo));
			} else {

				// Per default we assume the evaluatesTo type as success response type.
				// This could be overridden by according HttpProduces metadata. 
				contextBuilder.addResponseType(defaultSuccessCode, evaluatesTo);
			}

			// If configured adding specified successCodes (will be used to determine wasSuccessful())
			HttpSuccessCodes successCodesMd = this.entityResolver.meta(HttpSuccessCodes.T).exclusive();
			if (successCodesMd != null) {
				contextBuilder.addSuccessCodes(new HashSet<>(successCodesMd.getSuccessCodes()));
			}
			
			// If configured adding default success and failure types.
			HttpDefaultFailureResponseType defaultFailureTypeMd = this.entityResolver.meta(HttpDefaultFailureResponseType.T).exclusive();
			if (defaultFailureTypeMd != null) {
				contextBuilder.defaultFailureResponseType(resolveResponseType(defaultFailureTypeMd.getResponseTypeSignature()));
			}
			HttpDefaultSuccessResponseType defaultSuccessTypeMd = this.entityResolver.meta(HttpDefaultSuccessResponseType.T).exclusive();
			if (defaultSuccessTypeMd != null) {
				contextBuilder.defaultSuccessResponseType(resolveResponseType(defaultSuccessTypeMd.getResponseTypeSignature()));
			}
			
			// Adding individually specified response mappings (code to type)
			List<HttpProduces> producesMd = this.entityResolver.meta(HttpProduces.T).list();
			producesMd.stream()
				.forEach(m -> {
					contextBuilder.addResponseType(m.getResponseCode(),resolveResponseType(m.getResponseTypeSignature()));
					contextBuilder.addStatusCodeInfo(m.getResponseCode(), m.getUseOriginalStatusCode());
				});
			
		}
		
		
		
		
		private void indexPotentialResponseType(GenericModelType responseType) {
			switch (responseType.getTypeCode()) {
			case booleanType:
			case dateType:
			case decimalType:
			case doubleType:
			case floatType:
			case integerType:
			case longType:
			case stringType:
			case enumType:
			case objectType:
				break;
			case entityType:
				EntityType<?> entityType = (EntityType<?>) responseType;
				if (!responsePropertyTranslations.containsKey(entityType)) {
					PropertyTranslation propertyTranslation = new PropertyTranslation();
					responsePropertyTranslations.put(entityType, propertyTranslation);
					
					entityType.getProperties().forEach(p -> {
						//@formatter:off
						HttpParam md = 
								modelResolver
								.entityType(entityType)
								.property(p.getName())
								.meta(HttpParam.T)
								.exclusive();
						//@formatter:on
							String paramName = resolveParameterName(p, md);

							if (md instanceof HttpResourceStreamBodyParam) {
								propertyTranslation.streamContentResponseResourceProperty = p.getName();
							} else if (md == null || md.paramType() == HttpParamType.BODY) {
								propertyTranslation.bodyParameters.put(paramName, p);
							} else if (md.paramType() == HttpParamType.HEADER) {
								propertyTranslation.headerParameters.put(paramName, p);
							}

							indexPotentialResponseType(p.getType());
						});
					}
					break;
				case listType:
				case setType:
					CollectionType collectionType = (CollectionType) responseType;
					indexPotentialResponseType(collectionType.getCollectionElementType());
					break;
				case mapType:
					MapType mapType = (MapType) responseType;
					indexPotentialResponseType(mapType.getKeyType());
					indexPotentialResponseType(mapType.getValueType());
					break;
			}

		}

		private GenericModelType resolveResponseType(String responseTypeSignature) {
			String typeSignature = NullSafe.get(responseTypeSignature, "object");

			GenericModelType responseType = typeReflection.getType(typeSignature);
			indexPotentialResponseType(responseType);
			return responseType;
		}

		private <T> void resolve(Supplier<T> supplier, Consumer<T> consumer) {
			T result = supplier.get();
			if (result != null) {
				consumer.accept(result);
			}
		}

		private <T extends EntityTypeMetaData, R> R resolveMd(EntityType<T> mdType, Function<T, R> resolver) {
			T md = this.entityResolver.meta(mdType).exclusive();
			return (md != null) ? resolver.apply(md) : null;
		}

		private String resolveMethod(HttpMethod md) {
			return md.methodType();
		}

	}

	protected class PropertyTranslation {
		Map<String, Property> bodyParameters = new HashMap<>();
		Map<String, Property> headerParameters = new HashMap<>();
		String streamContentResponseResourceProperty;
	}

}
