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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.IdTypeSupplier;
import com.braintribe.codec.marshaller.api.IdentityManagementMode;
import com.braintribe.codec.marshaller.api.IdentityManagementModeOption;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.CompoundManipulation;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.VoidManipulation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.type.collection.ListTypeImpl;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.managed.NotFoundException;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.utils.lcd.ThrowableTools;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import hiconic.rx.web.rest.servlet.ManipulationTransformer;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import hiconic.rx.web.rest.servlet.RestV2Server;
import hiconic.rx.webapi.endpoints.v2.DdraManipulateEntitiesEndpoint;
import hiconic.rx.webapi.endpoints.v2.DdraUrlPathParameters;

/**
 * Handler used for POST, PUT and PATCH entities.
 */
public abstract class AbstractManipulateEntitiesHandler extends AbstractRestV2Handler<DdraManipulateEntitiesEndpoint> {

	private PersistenceGmSessionFactory sessionFactory;

	@Override
	protected DdraManipulateEntitiesEndpoint createEndpoint() {
		return DdraManipulateEntitiesEndpoint.T.create();
	}

	protected void createOrUpdate(RestV2EndpointContext<DdraManipulateEntitiesEndpoint> context, boolean allowCreateWithId,
			boolean allowCreateWithoutId, boolean allowMultipleEntities) throws IOException {

		DdraUrlPathParameters parameters = context.getParameters();
		EntityType<?> entityType = context.getEntityType();
		DdraManipulateEntitiesEndpoint endpoint = decode(context);

		ManipulationRequest request = ManipulationRequest.T.create();
		request.setDomainId(parameters.getAccessId());
		request.setSessionId(endpoint.getSessionId());

		ManipulationTransformer transformer = getManipulations(context, endpoint, entityType, allowMultipleEntities);
		List<GenericEntity> entities = computeEntitiesFromManipulations(transformer, parameters, endpoint);
		Set<GenericEntity> entitiesToCreate = computeEntitiesToCreate(context, entities, allowCreateWithId);
		List<Manipulation> manipulations = transformer.computeRemoteManipulations(entitiesToCreate, allowCreateWithoutId);
		request.setManipulation(computeManipulation(manipulations));

		try {
			ManipulationResponse response = evaluateServiceRequest(request, parameters.getEntityIdStringValue() != null);
			if (response.getInducedManipulation() != null)
				transformer.applyInducedManipulation(response.getInducedManipulation());

			writeResponse(context, project(context, endpoint, transformer, response), false);
		} catch (RuntimeException e) {
			Throwable rootCause = ThrowableTools.getRootCause(e);
			if (rootCause instanceof NotFoundException) {
				if (parameters.getEntityId() != null)
					HttpExceptions.throwNotFound(rootCause.getMessage());
				else
					HttpExceptions.throwBadRequest(rootCause.getMessage());
			}
			throw e;
		}
	}

	private ManipulationTransformer getManipulations(RestV2EndpointContext<DdraManipulateEntitiesEndpoint> context,
			DdraManipulateEntitiesEndpoint endpoint, EntityType<?> entityType, boolean allowMultipleEntities) {
		ManipulationTransformer manipulationTransformer = new ManipulationTransformer();
		PersistenceGmSession session = sessionFactory.newSession(context.getParameters().getAccessId());

		//@formatter:off
		GmDeserializationOptions options = GmDeserializationOptions.deriveDefaults()
				.setInferredRootType(endpoint.getListEntitiesRequest() ? new ListTypeImpl(entityType) : entityType)
				.setSession(manipulationTransformer.getSession())
				.set(IdTypeSupplier.class, session.getModelAccessory()::getIdType)
				.set(IdentityManagementModeOption.class, IdentityManagementMode.valueOf(endpoint.getIdentityManagementMode().name()))
				.build();
		//@formatter:on

		Object result = unmarshallBody(context, endpoint, options);
		boolean isCollection = result instanceof Collection;
		if (isCollection && !allowMultipleEntities)
			HttpExceptions.throwBadRequest("The body must only contain one entity.");

		manipulationTransformer.setTransformingMultipleEntities(isCollection);
		if (result != null && !isCollection)
			manipulationTransformer.setRequestEntityRuntimeId(((GenericEntity) result).runtimeId());

		return manipulationTransformer;
	}

	private List<GenericEntity> computeEntitiesFromManipulations(ManipulationTransformer manipulationTransformer, DdraUrlPathParameters parameters,
			DdraManipulateEntitiesEndpoint endpoint) {
		List<GenericEntity> entities = manipulationTransformer.getEntities();
		if (entities.isEmpty())
			HttpExceptions.throwBadRequest("No generic entity found in the post body.");
		else if (entities.size() > 1) {
			if (parameters.getEntityId() != null)
				entities.stream().filter(e -> e.getId() == null && e.type().getTypeSignature().equals(parameters.getTypeSignature())).findFirst()
						.ifPresent(p -> p.setId(parameters.getEntityId()));

			if (parameters.getEntityPartition() != null)
				entities.stream().filter(e -> e.getId() == null && e.type().getTypeSignature().equals(parameters.getTypeSignature())).findFirst()
						.ifPresent(p -> p.setPartition(parameters.getEntityPartition()));

			manipulationTransformer.setTransformingMultipleNestedEntities(true);

			if (endpoint.getId() != null)
				HttpExceptions.throwBadRequest("The body must only contain one entity because an id was specified in the URL path.");
		} else {
			GenericEntity decodedEntity = entities.iterator().next();
			if (parameters.getEntityId() != null)
				decodedEntity.setId(parameters.getEntityId());
			if (parameters.getEntityPartition() != null)
				decodedEntity.setPartition(parameters.getEntityPartition());
		}

		return entities;
	}

	private Set<GenericEntity> computeEntitiesToCreate(RestV2EndpointContext<?> context, List<GenericEntity> entities, boolean allowCreateWithId) {
		if (allowCreateWithId)
			return resolveEntitiesWithIdNotExists(context, entities);

		return entities.stream().filter(e -> e.getId() == null).collect(Collectors.toSet());
	}
	private Set<GenericEntity> resolveEntitiesWithIdNotExists(RestV2EndpointContext<?> context, List<GenericEntity> entities) {
		Set<GenericEntity> result = entities.stream().filter(e -> e.getId() != null).collect(Collectors.toSet());
		Set<Object> queriedEntities = queryEntities(context, result).stream().map(GenericEntity::getId).collect(Collectors.toSet());
		return entities.stream().filter(r -> !queriedEntities.contains(r.getId())).collect(Collectors.toSet());
	}

	private Manipulation computeManipulation(List<Manipulation> manipulations) {
		if (manipulations.isEmpty())
			return VoidManipulation.T.create();
		else if (manipulations.size() == 1)
			return manipulations.get(0);

		return getCompoundManipulation(manipulations);
	}
	private CompoundManipulation getCompoundManipulation(List<Manipulation> manipulations) {
		CompoundManipulation manipulation = CompoundManipulation.T.create();
		manipulation.setCompoundManipulationList(manipulations);
		return manipulation;
	}

	private Object project(RestV2EndpointContext<DdraManipulateEntitiesEndpoint> context, DdraManipulateEntitiesEndpoint endpoint,
			ManipulationTransformer transformer, ManipulationResponse response) {
		switch (endpoint.getProjection()) {
			case data:
				return getData(context, transformer);
			case envelope:
				return response;
			case idInfo:
				return collect(transformer, entity -> entity.getId());
			case locationInfo:
				return collect(transformer, entity -> getLocation(context, entity));
			case referenceInfo:
				return collect(transformer, entity -> entity.reference());
			case success:
				return true;
			default:
				HttpExceptions.throwInternalServerError("Unexpected projection %s", endpoint.getProjection());
				return null;
		}
	}

	private Object collect(ManipulationTransformer transformer, Function<GenericEntity, Object> transform) {
		if (transformer.isTransformingMultipleEntities())
			return transformer.getEntities().stream().map(transform).collect(Collectors.toList());
		if (transformer.isTransformingMultipleNestedEntities())
			return transformer.getEntities().stream().filter(e -> e.runtimeId() == transformer.getRequestEntityRuntimeId()).map(transform).findFirst()
					.get();

		return transformer.getEntities().stream().map(transform).findFirst().get();
	}

	private Object getData(RestV2EndpointContext<?> context, ManipulationTransformer transformer) {

		if (transformer.getEntities().isEmpty())
			return transformer.isTransformingMultipleEntities() ? Collections.emptyList() : null;

		List<GenericEntity> entities = queryEntities(context, new HashSet<>(transformer.getEntities()));

		if (transformer.isTransformingMultipleEntities())
			return entities;

		if (entities.isEmpty())
			return Collections.emptyList();

		return entities.get(0);
	}

	private List<GenericEntity> queryEntities(RestV2EndpointContext<?> context, Set<GenericEntity> entities) {
		PersistenceGmSession session = sessionFactory.newSession(context.getParameters().getAccessId());
		Map<EntityType<GenericEntity>, List<GenericEntity>> map = entities.stream().collect(Collectors.groupingBy(GenericEntity::entityType));

		return map.entrySet().stream().map(entry -> {
			EntityQuery query = EntityQueryBuilder.from(entry.getKey()).where().entity(EntityQueryBuilder.DEFAULT_SOURCE)
					.in(referencesForEntities(entry.getValue())).done();

			return session.query().entities(query).<GenericEntity> list();
		}).flatMap(List::stream).collect(Collectors.toList());
	}

	private String getLocation(RestV2EndpointContext<?> context, GenericEntity entity) {
		context.getParameters().setProperty(null);
		context.getParameters().setEntityIdStringValue(entity.getId().toString());
		context.getParameters().setPartition(entity.getPartition());

		return RestV2Server.getUrlFor(context.getRequest(), context.getParameters());
	}

	@Required
	@Configurable
	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
