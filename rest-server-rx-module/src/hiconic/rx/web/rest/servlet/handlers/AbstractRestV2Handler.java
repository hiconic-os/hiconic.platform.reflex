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
package hiconic.rx.web.rest.servlet.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.MarshallException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.logging.Logger;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.PropertyManipulation;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.EntityReferenceType;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.processing.session.api.managed.NotFoundException;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.ThrowableTools;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoder;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;
import dev.hiconic.servlet.decoder.api.StandardHeadersMapper;
import hiconic.rx.web.ddra.endpoints.api.DdraEndpointContext;
import hiconic.rx.web.ddra.endpoints.api.DdraEndpointsUtils;
import hiconic.rx.web.ddra.endpoints.api.DdraTraversingCriteriaMap;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import hiconic.rx.webapi.endpoints.DdraEndpointHeaders;
import hiconic.rx.webapi.endpoints.v2.DdraUrlPathParameters;
import hiconic.rx.webapi.endpoints.v2.RestV2Endpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractRestV2Handler<E extends RestV2Endpoint> implements RestV2Handler<E> {

	private static final StandardHeadersMapper<DdraEndpointHeaders> ENDPOINT_MAPPER = StandardHeadersMapper.mapToProperties(DdraEndpointHeaders.T);

	private static final Logger logger = Logger.getLogger(AbstractRestV2Handler.class);

	protected Evaluator<ServiceRequest> evaluator;

	protected MarshallerRegistry marshallerRegistry;

	protected DdraTraversingCriteriaMap traversingCriteriasMap;
	
	protected abstract E createEndpoint();

	protected E decode(RestV2EndpointContext<E> context) {
		return decode(context, null, null, HttpRequestEntityDecoderOptions.defaults());
	}

	protected E decode(RestV2EndpointContext<E> context, String extraEntityPrefix, GenericEntity extraEntity) {
		return decode(context, extraEntityPrefix, extraEntity, HttpRequestEntityDecoderOptions.defaults());
	}

	protected E decode(RestV2EndpointContext<E> context, String extraEntityPrefix, GenericEntity extraEntity,
			HttpRequestEntityDecoderOptions options) {
		
		E endpoint = context.getEndpoint();
		
		HttpRequestEntityDecoder decoder = HttpRequestEntityDecoder.createFor(context.getRequest(), options)
				.target("endpoint", endpoint , ENDPOINT_MAPPER);

		if (extraEntityPrefix != null)
			decoder = decoder.target(extraEntityPrefix, extraEntity);

		decoder.decode();
		DdraEndpointsUtils.computeDepth(endpoint);
		computeOutMarshallerFor(context);

		return endpoint;
	}

	protected PersistentEntityReference getEntityReferenceFor(RestV2EndpointContext<?> context) {
		DdraUrlPathParameters parameters = context.getParameters();
		PersistentEntityReference reference = PersistentEntityReference.T.createPlain();
		reference.setRefId(parameters.getEntityId());

		reference.setTypeSignature(context.getEntityType().getTypeSignature());
		if (parameters.getEntityPartition() != null)
			reference.setRefPartition(parameters.getEntityPartition());
		else if (reference.getRefPartition() == null)
			reference.setRefPartition(EntityReference.ANY_PARTITION);

		return reference;
	}

	protected Object unmarshallBody(RestV2EndpointContext<E> context, E endpoint, GmDeserializationOptions options) {
		try (InputStream in = context.getRequest().getInputStream()) {
			Marshaller marshaller = DdraEndpointsUtils.getInMarshallerFor(marshallerRegistry, endpoint);
			return marshaller.unmarshall(in, options);
		} catch (IOException | MarshallException e) {
			logger.warn("Error while unmarshalling body for request " + context.getRequest().getPathInfo(), e);
			HttpExceptions.throwBadRequest("Error while unmarshalling body, reason: %s", e.getMessage());
			return null;
		}
	}

	protected void computeOwnerForPropertyManipulation(PropertyManipulation manipulation, RestV2EndpointContext<?> context) {
		EntityProperty owner = EntityProperty.T.create();
		manipulation.setOwner(owner);
		owner.setReference(getEntityReferenceFor(context));
		owner.setPropertyName(context.getParameters().getProperty());
	}

	protected Object getReferenceForPropertyValue(GenericModelType type, Object value, Property property) {
		if (value != null && !type.isInstance(value))
			HttpExceptions.throwBadRequest("Unexpected value type, expected %s but got %s for property %s.", type.getJavaType().getName(),
					value.getClass().getName(), property.getName());

		if (GenericEntity.T.isInstance(value))
			return getPersistenceEntityReferenceFor((GenericEntity) value);

		return value;
	}

	private PersistentEntityReference getPersistenceEntityReferenceFor(GenericEntity entity) {
		EntityReference result;
		if (EntityReference.T.isInstance(entity))
			result = (EntityReference) entity;
		else
			result = entity.reference();

		checkIsPersistentReference(result);

		if (result.getRefPartition() == null)
			result.setRefPartition(EntityReference.ANY_PARTITION);

		return (PersistentEntityReference) result;
	}

	protected void checkIsPersistentReference(Object reference) {
		if (!PersistentEntityReference.T.isInstance(reference))
			HttpExceptions.throwBadRequest("Only simple properties, or PersistentEntityReference  are allowed for PUT /properties");
	}

	protected ManipulationRequest createManipulationRequestFor(DdraUrlPathParameters parameters, RestV2Endpoint endpoint) {
		ManipulationRequest request = ManipulationRequest.T.create();
		request.setDomainId(parameters.getAccessId());
		request.setSessionId(endpoint.getSessionId());

		return request;
	}

	protected <T> T evaluateServiceRequest(ServiceRequest service, boolean throw404OnNotFound) {
		try {
			return DdraEndpointsUtils.evaluateServiceRequest(evaluator, service);
		} catch (RuntimeException e) {
			if (throw404OnNotFound) {
				NotFoundException notFound = ThrowableTools.searchCause(e, NotFoundException.class);
				if (notFound != null) {
					HttpExceptions.throwNotFound(notFound.getMessage());
				}
			}
			throw e;
		}
	}

	public static Set<EntityReference> referencesForEntities(List<GenericEntity> entities) {
		return entities.stream().map(AbstractRestV2Handler::entityReference).collect(Collectors.toSet());
	}

	public static EntityReference entityReference(GenericEntity entity) {
		EntityReference reference = entity.reference();
		if (reference.getRefPartition() == null && reference.referenceType() == EntityReferenceType.persistent)
			reference.setRefPartition(EntityReference.ANY_PARTITION);
		return reference;
	}

	protected void writeResponse(DdraEndpointContext<?> context, Object result, boolean full) throws IOException {
		DdraEndpointsUtils.writeResponse(traversingCriteriasMap, context, result, full);
	}

	protected void computeOutMarshallerFor(DdraEndpointContext<E> context) {
		DdraEndpointsUtils.computeOutMarshallerFor(marshallerRegistry, context, null);
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
	public void setTraversingCriteriaMap(DdraTraversingCriteriaMap traversingCriteriaMap) {
		this.traversingCriteriasMap = traversingCriteriaMap;
	}
	
	@Override
	public RestV2EndpointContext<E> createContext(HttpServletRequest request, HttpServletResponse response) {
		RestV2EndpointContext<E> context = new RestV2EndpointContext<>(request, response);
		context.setEndpoint(createEndpoint());
		return context;
	}
}
