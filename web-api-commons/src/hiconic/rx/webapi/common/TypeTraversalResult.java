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
package hiconic.rx.webapi.common;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;

public class TypeTraversalResult {
	private final String prefix;
	private final Property property;
	private final EntityType<?> entityType;
	private final TypeTraversalResult parent;

	public TypeTraversalResult(Property property, EntityType<?> entityType) {
		this(property, entityType, null);
	}
	
	public TypeTraversalResult(Property property, EntityType<?> entityType, String prefix) {
		this.parent = null;
		this.prefix = prefix;
		this.property = property;
		this.entityType = entityType;
	}
	
	public TypeTraversalResult(TypeTraversalResult parent, Property property, EntityType<?> entityType) {
		this.parent = parent;
		this.prefix = parent == null ? null : parent.prefixedPropertyName();
		this.property = property;
		this.entityType = entityType;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public Property getProperty() {
		return property;
	}
	
	public TypeTraversalResult getParent() {
		return parent;
	}
	
	public EntityType<?> getEntityType() {
		return entityType;
	}
	
	public String prefixedPropertyName() {
		if (prefix == null){
			return property.getName();
		}
		
		return prefix  + "." + property.getName();
	}

}