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
package hiconic.rx.platform.cli.processing;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.Property;

public class ConstraintViolation {
	private GenericEntity entity;
	private Property property;
	private Object value;
	private String message;

	public ConstraintViolation(GenericEntity entity, Property property, Object value, String message) {
		this.entity = entity;
		this.property = property;
		this.value = value;
		this.message = message;
	}

	public GenericEntity getEntity() {
		return entity;
	}

	public Property getProperty() {
		return property;
	}

	public Object getValue() {
		return value;
	}

	public String getMessage() {
		return message;
	}
}
