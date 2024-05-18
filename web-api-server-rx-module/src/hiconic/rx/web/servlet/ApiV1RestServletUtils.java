// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package hiconic.rx.web.servlet;

import static com.braintribe.model.processing.web.rest.HttpExceptions.notFound;
import static com.braintribe.utils.lcd.CollectionTools2.first;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.braintribe.cfg.Configurable;
import com.braintribe.ddra.TypeTraversal;
import com.braintribe.ddra.TypeTraversalResult;
import com.braintribe.logging.Logger;
import com.braintribe.model.ddra.endpoints.OutputPrettiness;
import com.braintribe.model.ddra.endpoints.TypeExplicitness;
import com.braintribe.model.ddra.endpoints.api.v1.ApiV1DdraEndpoint;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.tools.GmModelTools;
import com.braintribe.model.meta.GmCustomType;
import com.braintribe.model.meta.data.prompt.Embedded;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.rpc.commons.impl.RpcMarshallingStreamManagement;
import com.braintribe.model.processing.rpc.commons.impl.RpcUnmarshallingStreamManagement;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.api.MimeTypeRegistry;
import com.braintribe.model.resource.source.TransientSource;
import com.braintribe.model.service.api.DomainRequest;
import com.braintribe.model.service.api.HasServiceRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.lcd.FileTools;
import com.braintribe.utils.lcd.StringTools;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.web.multipart.api.FormDataWriter;
import com.braintribe.web.multipart.api.MutablePartHeader;
import com.braintribe.web.multipart.api.PartReader;
import com.braintribe.web.multipart.impl.Multiparts;

import dev.hiconic.servlet.ddra.endpoints.api.api.v1.ApiV1EndpointContext;
import dev.hiconic.servlet.ddra.endpoints.api.api.v1.SingleDdraMapping;
import dev.hiconic.servlet.decoder.api.DecoderTargetRegistry;
import dev.hiconic.servlet.decoder.api.HttpExceptions;

public class ApiV1RestServletUtils {
	private final static Logger logger = Logger.getLogger(ApiV1RestServletUtils.class);

	private MimeTypeRegistry mimeTypeRegistry = null;

	public void ensureServiceDomain(ServiceRequest service, ApiV1EndpointContext context) {
		if (service instanceof DomainRequest) {
			DomainRequest domainRequest = (DomainRequest) service;
			if (domainRequest.getDomainId() == null) {
				domainRequest.setDomainId(context.getServiceDomain());
			} else {
				context.setServiceDomain(domainRequest.getDomainId());
			}
		} /* else if (service instanceof DispatchableRequest) { DispatchableRequest dispatchableRequest =
			 * (DispatchableRequest) service; if (dispatchableRequest.getServiceId() == null) {
			 * dispatchableRequest.setServiceId(context.getParameters().getServiceDomain()); } } */
	}

	public Object project(ApiV1EndpointContext context, ApiV1DdraEndpoint endpoint, Object result) {
		if (result == null)
			return result;

		String projection = endpoint.getProjection();
		if (StringTools.isEmpty(projection) && context.getMapping() != null)
			projection = context.getMapping().getDefaultProjection();

		if (endpoint.allProjection(projection))
			return result;

		if (result instanceof GenericEntity) {
			String[] properties = projection.split(Pattern.quote("."));
			Property property;

			EntityType<?> parentType = ((GenericEntity) result).entityType();
			GenericModelType currentType = parentType;

			for (String propertyName : properties) {
				if (!(currentType instanceof EntityType))
					HttpExceptions.badRequest("Invalid projection %s - the property %s in entityType %s is not an entityType", projection,
							propertyName, parentType.getTypeSignature());

				EntityType<?> entityType = (EntityType<?>) currentType;
				property = entityType.findProperty(propertyName);
				if (property == null)
					HttpExceptions.badRequest("Invalid projection %s - no property found with name %s in entityType %s", projection, propertyName,
							entityType.getTypeSignature());

				parentType = entityType;
				currentType = property.getType();
				result = property.get((GenericEntity) result);
			}

			context.setExpectedResponseType(currentType);

			return result;
		}

		HttpExceptions.badRequest("Got projection %s but the result was not a GenericEntity, result class: %s", projection,
				result.getClass().getName());

		return null;
	}

	public boolean isSimpleTypeName(String typeSignature) {
		return !typeSignature.contains(".");
	}

	public EntityType<? extends ServiceRequest> resolveTypeFromSignature(String typeSignature, ModelOracle modelOracle) {
		EntityType<? extends ServiceRequest> entityType = GMF.getTypeReflection().findType(typeSignature);
		if (entityType != null || !isSimpleTypeName(typeSignature))
			return entityType;

		List<GmCustomType> matchingTypes = modelOracle.findGmTypeBySimpleName(typeSignature);
		if (matchingTypes.isEmpty())
			return null;

		if (matchingTypes.size() > 1) {
			notFound("Cannot find request with type signature %s, but multiple requests found with given simple name.", typeSignature,
					matchingTypes.toString());
		}

		return first(matchingTypes).reflectionType();
	}

	public ServiceRequest computeTransformRequest(ApiV1EndpointContext context, ServiceRequest delegate) {
		SingleDdraMapping mapping = context.getMapping();
		if (mapping == null || mapping.getTransformRequest() == null) {
			return delegate;
		}

		ServiceRequest service = mapping.getTransformRequest();

		if (HasServiceRequest.T.isAssignableFrom(service.entityType()) && delegate != null)
			((HasServiceRequest) service).setServiceRequest(delegate);

		return service;
	}

	public boolean isEmbedded(GenericEntity entity, Property property, ModelMdResolver metaDataResolver) {
		return metaDataResolver //
				.entity(entity).property(property) //
				.meta(Embedded.T) //
				.exclusive() != null;
	}

	public class DecodingTargetTraversalResult extends TypeTraversalResult {
		private GenericEntity ownEntity;
		private Runnable onSetValue = () -> {
			/* noop */}; // gets executed when any property of ownEntity gets assigned a value

		final GenericEntity parentEntity;
		private final DecoderTargetRegistry decoder;
		final DecodingTargetTraversalResult parent; // shadowing for type specificity

		public DecodingTargetTraversalResult(Property property, EntityType<?> entityType, DecoderTargetRegistry decoder, GenericEntity parentEntity) {
			super(null, property, entityType);
			this.decoder = decoder;
			this.parentEntity = parentEntity;
			this.parent = null;
		}

		public DecodingTargetTraversalResult(DecodingTargetTraversalResult parent, Property property, EntityType<?> entityType) {
			super(parent, property, entityType);
			this.decoder = parent.decoder;
			this.parent = parent;

			parent.ensureOwnEntity(true);

			this.parentEntity = parent.ownEntity;
		}

		void ensureOwnEntity(boolean registerDecodingTarget) {

			if (ownEntity != null) {
				return;
			}

			// In the moment when a TypeTraversalResult becomes a parent we know its property is of type GenericEntity.
			// So here is no type check necessary
			ownEntity = getProperty().get(parentEntity);

			if (ownEntity == null) {
				EntityType<?> propertyType = (EntityType<?>) getProperty().getType();
				GenericEntity createdEntity = GmModelTools.createShallow(propertyType);
				ownEntity = createdEntity;

				Runnable onSetValueAlsoSetParentPropertyToThisEntity = () -> getProperty().set(parentEntity, ownEntity);

				if (parent == null) {
					onSetValue = onSetValueAlsoSetParentPropertyToThisEntity;
				} else {
					onSetValue = () -> {
						parent.onSetValue.run();
						onSetValueAlsoSetParentPropertyToThisEntity.run();
					};
				}
			}

			if (registerDecodingTarget) {
				decoder.target(prefixedPropertyName(), ownEntity, onSetValue);
			}
		}

		public GenericEntity ensureOwnEntity() {
			ensureOwnEntity(false);
			onSetValue.run();
			return ownEntity;
		}

		public Object getOwnValue() {
			if (ownEntity == null) {
				return getProperty().get(parentEntity);
			}
			return ownEntity;
		}

	}

	public List<DecodingTargetTraversalResult> traverseDecodingTarget(GenericEntity entity, DecoderTargetRegistry decoder,
			ModelMdResolver metaDataResolver) {
		return TypeTraversal.traverseType(metaDataResolver, entity.entityType(),
				(DecodingTargetTraversalResult parent, Property property, EntityType<?> e) -> //
				parent == null //
						? new DecodingTargetTraversalResult(property, e, decoder, entity) //
						: new DecodingTargetTraversalResult(parent, property, e));
	}

	public ApiV1DdraEndpoint createDefaultEndpoint(SingleDdraMapping mapping) {
		if (mapping == null) {
			return ApiV1DdraEndpoint.T.create();
		}

		ApiV1DdraEndpoint endpoint = mapping.getEndpointPrototype();

		if (endpoint == null) {
			endpoint = ApiV1DdraEndpoint.T.create();
		}

		if (mapping.getDefaultDepth() != null) {
			endpoint.setDepth(mapping.getDefaultDepth());
		}
		if (mapping.getDefaultResponseContentType() != null) {
			endpoint.setResponseContentType(mapping.getDefaultResponseContentType());
		}
		if (mapping.getDefaultSaveLocally() != null) {
			endpoint.setSaveLocally(mapping.getDefaultSaveLocally());
		}
		if (mapping.getDefaultResponseFilename() != null) {
			endpoint.setResponseFilename(mapping.getDefaultResponseFilename());
		}
		if (mapping.getDefaultDownloadResource() != null) {
			endpoint.setDownloadResource(mapping.getDefaultDownloadResource());
		}
		if (mapping.getDefaultWriteAbsenceInformation() != null) {
			endpoint.setWriteAbsenceInformation(mapping.getDefaultWriteAbsenceInformation());
		}
		if (mapping.getDefaultStabilizeOrder() != null) {
			endpoint.setStabilizeOrder(mapping.getDefaultStabilizeOrder());
		}
		if (mapping.getDefaultWriteEmptyProperties() != null) {
			endpoint.setWriteEmptyProperties(mapping.getDefaultWriteEmptyProperties());
		}
		if (mapping.getDefaultEntityRecurrenceDepth() != null) {
			endpoint.setEntityRecurrenceDepth(mapping.getDefaultEntityRecurrenceDepth());
		}
		if (mapping.getDefaultPrettiness() != null) {
			try {
				endpoint.setPrettiness(OutputPrettiness.valueOf(mapping.getDefaultPrettiness()));
			} catch (Exception e) {
				logger.warn("Invalid defaultPrettiness configured on DdraMapping: " + mapping.getDefaultPrettiness());
			}
		}
		if (mapping.getDefaultTypeExplicitness() != null) {
			try {
				endpoint.setTypeExplicitness(TypeExplicitness.valueOf(mapping.getDefaultTypeExplicitness()));
			} catch (Exception e) {
				logger.warn("Invalid typeExplicitness configured on DdraMapping: " + mapping.getDefaultTypeExplicitness());
			}
		}

		if (mapping.getDefaultUseSessionEvaluation() != null) {
			endpoint.setUseSessionEvaluation(mapping.getDefaultUseSessionEvaluation());
		}
		return endpoint;
	}

	public void writeOutTransientSources(ApiV1EndpointContext context, FormDataWriter formDataWriter) throws IOException {
		RpcMarshallingStreamManagement responseStreamManagement = context.getResponseStreamManagement();
		for (TransientSource transientSource : responseStreamManagement.getTransientSources()) {
			MutablePartHeader header = Multiparts.newPartHeader();
			header.setName(transientSource.getGlobalId());
			Resource owner = transientSource.getOwner();
			if (owner != null) {
				transfer(owner::getMimeType, header::setContentType);
				transfer(owner::getFileSize, header::setContentLength);
				transfer(owner::getName, header::setFileName);
			}

			try (OutputStream part = formDataWriter.openPart(header).outputStream()) {
				transientSource.writeToStream(part);
			}
		}
	}

	public void transfer(Supplier<?> supplier, Consumer<String> consumer) {
		Object gotten = supplier.get();
		if (gotten != null) {
			consumer.accept(gotten.toString());
		}
	}
	public Resource processResourcePart(RpcUnmarshallingStreamManagement streamManagement, PartReader part, TransientSource transientSource) {
		Resource owner = transientSource.getOwner();
		if (owner != null) {
			String partFileName = part.getFileName();
			if (owner.getName() == null) {
				owner.setName(partFileName);
			}

			if (owner.getMimeType() == null) {
				String partContentType = part.getContentType();
				if (partContentType != null) {
					if (partContentType.toLowerCase().startsWith("application/octet-stream")) {
						partContentType = null;
					} else if (!StringTools.isBlank(partFileName)) {
						String extension = FileTools.getExtension(partFileName);
						if (!StringTools.isBlank(extension)) {
							String partMimeType = extractMimeFromContentType(partContentType);
							if (partMimeType != null) {
								Collection<String> acceptedExtensions = mimeTypeRegistry.getExtensions(partMimeType);
								if (acceptedExtensions != null && !acceptedExtensions.isEmpty()) {
									if (!acceptedExtensions.contains(extension)) {
										partContentType = null;
									}
								}
							}
						}
					}
				}
				owner.setMimeType(partContentType);
			}

			String contentLength = part.getContentLength();
			if (owner.getFileSize() == null && contentLength != null) {
				try {
					owner.setFileSize(Long.valueOf(contentLength));
				} catch (NumberFormatException e) {
					logger.warn("Could not parse content length header of part '" + part.getName() + "' as long: '" + contentLength + "'.");
				}
			}
		}

		String globalId = transientSource.getGlobalId();
		StreamPipe pipe = streamManagement.acquirePipe(globalId);
		transientSource.setInputStreamProvider(pipe::openInputStream);

		try (OutputStream out = pipe.openOutputStream(); InputStream openStream = part.openStream()) {
			IOTools.transferBytes(openStream, out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return owner;
	}
	
	private String extractMimeFromContentType(String contentType) {
		int index = contentType.indexOf(';');
		
		if (index == -1)
			return contentType;
		
		return contentType.substring(0, index).trim();
	}

	@Configurable
	public void setMimeTypeRegistry(MimeTypeRegistry mimeTypeRegistry) {
		this.mimeTypeRegistry = mimeTypeRegistry;
	}

}
