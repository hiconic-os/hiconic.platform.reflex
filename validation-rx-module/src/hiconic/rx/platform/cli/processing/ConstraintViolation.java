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
