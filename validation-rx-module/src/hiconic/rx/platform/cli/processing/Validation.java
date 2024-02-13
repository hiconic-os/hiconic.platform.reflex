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

import java.util.ArrayList;
import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.meta.data.constraint.Mandatory;
import com.braintribe.model.processing.meta.cmd.CmdResolver;

public class Validation {
	private final CmdResolver cmdResolver;

	private final List<PropertyValidator> propertyValidators = new ArrayList<>();

	public Validation(CmdResolver cmdResolver) {
		this.cmdResolver = cmdResolver;

		// propertyValidators.add(this::validateRegex);
		// propertyValidators.add(this::validateMin);
		// propertyValidators.add(this::validateMax);
		// propertyValidators.add(this::validateMinLength);
		// propertyValidators.add(this::validateMaxLength);
		propertyValidators.add(this::validateMandatory);
	}

	public ValidationProtocol validate(GenericEntity entity) {

		ValidationProtocol protocol = new ValidationProtocol();

		for (Property property : entity.entityType().getProperties()) {
			Object value = property.get(entity);

			for (PropertyValidator validator : propertyValidators) {
				String violationMessage = validator.validate(entity, property, value);

				if (violationMessage != null) {
					ConstraintViolation violation = new ConstraintViolation(entity, property, value, violationMessage);
					protocol.getViolations().add(violation);
				}
			}
		}

		return protocol;
	}

	// private String validateRegex(GenericEntity entity, Property property, Object value) {
	// return null;
	// }
	//
	// private String validateMin(GenericEntity entity, Property property, Object value) {
	// return null;
	// }
	//
	// private String validateMax(GenericEntity entity, Property property, Object value) {
	// return null;
	// }
	//
	// private String validateMinLength(GenericEntity entity, Property property, Object value) {
	// return null;
	// }
	//
	// private String validateMaxLength(GenericEntity entity, Property property, Object value) {
	// return null;
	// }

	private String validateMandatory(GenericEntity entity, Property property, Object value) {
		boolean mandatory = cmdResolver.getMetaData().entity(entity).property(property).is(Mandatory.T);

		if (mandatory && value == null) {
			return "Property is mandatory";
		}

		return null;
	}
}
