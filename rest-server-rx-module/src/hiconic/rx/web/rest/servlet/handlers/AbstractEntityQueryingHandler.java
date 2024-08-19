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

import static com.braintribe.utils.lcd.CollectionTools2.first;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.ddra.endpoints.v2.RestV2Endpoint;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.data.constraint.TypeSpecification;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.PropertyMdResolver;
import com.braintribe.model.processing.query.fluent.AbstractQueryBuilder;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.query.fluent.JunctionBuilder;
import com.braintribe.model.query.EntityQuery;

import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;
import dev.hiconic.servlet.decoder.api.PropertyTypeResolver;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import hiconic.rx.web.rest.servlet.RestV2EntityQueryBuilder;

public abstract class AbstractEntityQueryingHandler<E extends RestV2Endpoint> extends AbstractQueryingHandler<E> {

	private AccessDomains accessDomains;
	private final Map<EntityType<?>, EntityType<?>> typeToInstantiableSubType = new ConcurrentHashMap<>();
	
	protected AbstractQueryBuilder<EntityQuery> decodeEntityQueryBuilder(RestV2EndpointContext<E> context) {
		EntityType<?> entityType = context.getEntityType();
		
		EntityType<?> instantiableSybType = findInstantiableSybTypeOf(entityType, context);
		if (instantiableSybType == null) {
			decode(context, null, null, HttpRequestEntityDecoderOptions.defaults().addIgnoredParameter("where"));
			// No instantiable sub type exists, so we simply use a condition that always evaluates to false
			return noEntityMatchingQuerye(entityType);

		} else { 
			return decodeEntityQueryBuilderWithType(context, instantiableSybType);
		}
	}

	private EntityType<?> findInstantiableSybTypeOf(EntityType<?> entityType, RestV2EndpointContext<?> context) {
		if (!entityType.isAbstract())
			return entityType;
		
		return typeToInstantiableSubType.computeIfAbsent(entityType, et -> findInstantiableSubType(et, context));
	}
	
	private EntityType<?> findInstantiableSubType(EntityType<?> et, RestV2EndpointContext<?> context) {
		String accessId = context.getParameters().getAccessId();
		CmdResolver cmdResolver = accessDomains.byId(accessId).configuredDataModel().contextCmdResolver();
		
		Set<EntityType<?>> instantiableSubTypes = cmdResolver.getModelOracle().getEntityTypeOracle(et).getSubTypes().onlyInstantiable().asTypes();
		return instantiableSubTypes.isEmpty() ? null : first(instantiableSubTypes);
	}

	private AbstractQueryBuilder<EntityQuery> decodeEntityQueryBuilderWithType(RestV2EndpointContext<E> context,
			EntityType<?> instantiableSybType) {

		EntityQueryBuilder builder = EntityQueryBuilder.from(context.getEntityType());

		JunctionBuilder<? extends AbstractQueryBuilder<EntityQuery>> conjunction = builder.where().conjunction();

		GenericEntity entity = instantiableSybType.createRaw(new RestV2EntityQueryBuilder(conjunction));
		decode(context, "where", entity,
				HttpRequestEntityDecoderOptions.defaults().setPropertyTypeResolver(getPropertyResolver(context.getParameters().getAccessId())));

		conjunction.close();

		return builder;
	}

	private AbstractQueryBuilder<EntityQuery> noEntityMatchingQuerye(EntityType<?> entityType) {
		return EntityQueryBuilder.from(entityType).where().value(0).eq(1);
	}

	private PropertyTypeResolver getPropertyResolver(String accessId) {
		CmdResolver cmdResolver = accessDomains.byId(accessId).configuredDataModel().contextCmdResolver();

		return (entityType, property) -> {
			PropertyMdResolver mdResolver = cmdResolver.getMetaData().entityType(entityType).property(property.getName());
			TypeSpecification ts = mdResolver.meta(TypeSpecification.T).exclusive();
			return ts != null ? ts.getType().reflectionType() : null;
		};
	}

	@Required
	@Configurable
	public void setAccessDomains(AccessDomains accessDomains) {
		this.accessDomains = accessDomains;
	}
}
