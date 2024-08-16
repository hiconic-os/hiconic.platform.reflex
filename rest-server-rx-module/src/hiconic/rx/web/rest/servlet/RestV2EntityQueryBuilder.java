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

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.PropertyAccessInterceptor;
import com.braintribe.model.processing.query.fluent.AbstractQueryBuilder;
import com.braintribe.model.processing.query.fluent.JunctionBuilder;
import com.braintribe.model.query.EntityQuery;

public class RestV2EntityQueryBuilder extends PropertyAccessInterceptor {

	private final JunctionBuilder<? extends AbstractQueryBuilder<EntityQuery>> conjunction;
	
	public RestV2EntityQueryBuilder(JunctionBuilder<? extends AbstractQueryBuilder<EntityQuery>> conjunction) {
		this.conjunction = conjunction;
	}
	
	@Override
	public Object getProperty(Property property, GenericEntity entity, boolean isVd) {
		Object value = property.getDirect(entity);
		
		if (!property.getType().isScalar()) {
			throw new IllegalArgumentException("Illegal query parameter: 'where." + property.getName() + "'. Only scalar types are allowed for where-parameters but property '" + property.getName() + "' of an entity with type '" + entity.entityType().getTypeSignature() + "' is itself of type: '" + property.getType().getTypeSignature() + "'.");
		}
		return value;
	}
	
	@Override
	public Object setProperty(Property property, GenericEntity entity, Object value, boolean isVd) {
		this.conjunction.property(property.getName()).eq(value);
		return property.setDirect(entity, value);
	}
}
