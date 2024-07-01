// ============================================================================
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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.meta.data.UniversalMetaData;
import com.braintribe.model.meta.data.constraint.Limit;
import com.braintribe.model.meta.data.constraint.Mandatory;
import com.braintribe.model.meta.data.constraint.Max;
import com.braintribe.model.meta.data.constraint.MaxLength;
import com.braintribe.model.meta.data.constraint.Min;
import com.braintribe.model.meta.data.constraint.MinLength;
import com.braintribe.model.meta.data.constraint.Pattern;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.utils.StringTools;

import hiconic.rx.platform.cli.processing.Validation.InstancePropertyValidator;

public class Validation {
	private final CmdResolver cmdResolver;
	private final static Logger logger = System.getLogger(Validation.class.getName());

	private final List<PropertyValidator> propertyValidators = new ArrayList<>();
	private final Map<EntityType<?>, Map<Property, List<InstancePropertyValidator>>> cachedValidators = new ConcurrentHashMap<>();

	public Validation(CmdResolver cmdResolver) {
		this.cmdResolver = cmdResolver;

		propertyValidators.add(this::validateRegex);
		propertyValidators.add(this::validateMin);
		propertyValidators.add(this::validateMax);
		propertyValidators.add(this::validateMinLength);
		propertyValidators.add(this::validateMaxLength);
		propertyValidators.add(this::validateMandatory);
	}

	public ValidationProtocol validate(GenericEntity entity) {

		ValidationProtocol protocol = new ValidationProtocol();

		EntityType<GenericEntity> entityType = entity.entityType();
		Map<Property, List<InstancePropertyValidator>> validators = cachedValidators.computeIfAbsent(entityType,
				this::precompileValidators);

		for (Map.Entry<Property, List<InstancePropertyValidator>> entry : validators.entrySet()) {
			Property property = entry.getKey();

			Object value = property.get(entity);

			for (InstancePropertyValidator validator : entry.getValue()) {
				String violationMessage = validator.validate(entity, property, value);

				if (violationMessage != null) {
					ConstraintViolation violation = new ConstraintViolation(entity, property, value, violationMessage);
					protocol.getViolations().add(violation);
				}
			}
		}

		return protocol;
	}

	private Map<Property, List<InstancePropertyValidator>> precompileValidators(EntityType<?> entityType) {
		Map<Property, List<InstancePropertyValidator>> instanceValidators = new LinkedHashMap<>();

		for (Property property : entityType.getProperties()) {
			List<InstancePropertyValidator> instancePropertyValidators = new ArrayList<>();

			for (PropertyValidator validator : propertyValidators) {
				InstancePropertyValidator instancePropertyValidator = validator.validate(entityType, property);

				if (instancePropertyValidator != null)
					instancePropertyValidators.add(instancePropertyValidator);
			}

			instanceValidators.put(property, instancePropertyValidators);
		}

		return instanceValidators;
	}

	private InstancePropertyValidator validateRegex(EntityType<?> entityType, Property property) {
		Pattern pattern = cmdResolver.getMetaData().entityType(entityType).property(property).meta(Pattern.T)
				.exclusive();
		if (pattern == null)
			return null;

		String expression = pattern.getExpression();
		if (expression == null)
			return null;
		
		java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(expression);

		GenericModelType propertyType = property.getType();
		if (propertyType != EssentialTypes.TYPE_STRING) {
			// TODO: what to return here? error? log?
			logger.log(Level.WARNING,
					"Pattern metadata should only be applied on string-typed properties. Found on "
							+ entityType.getShortName() + "." + property.getName() + " of type "
							+ propertyType.getTypeSignature());
			// temporarily lenient error-handling
			return null;
		}

		return (entity, p, value) -> {
			if (value == null)
				return null;

			String stringValue = (String) value;
			
			if (compiledPattern.matcher(stringValue).matches())
				return null;
			
			return "Value '" + StringTools.truncateIfRequired(stringValue, 100) + "' does not match regex pattern "
					+ StringTools.truncateIfRequired(expression, 100);
		};
	}
	
	private InstancePropertyValidator validateMin(EntityType<?> entityType, Property property) {
		return validateLimit(Min.T, entityType, property);
	}

	private InstancePropertyValidator validateMax(EntityType<?> entityType, Property property) {
		return validateLimit(Max.T, entityType, property);
	}

	private InstancePropertyValidator validateLimit(EntityType<? extends Limit> limitType, EntityType<?> entityType,
			Property property) {
		Limit limitMd = cmdResolver.getMetaData().entityType(entityType).property(property).meta(limitType).exclusive();
		if (limitMd == null)
			return null;

		GenericModelType propertyType = property.getType();
		String shortName = limitType.getShortName();
		if (!propertyType.isScalar()) {
			// TODO: what to return here? error? log?
			logger.log(Level.WARNING,
					shortName + " metadata should only be applied on scalar-typed properties. Found on "
							+ entityType.getShortName() + "." + property.getName() + " of type "
							+ propertyType.getTypeSignature());
			// temporarily lenient error-handling
			return null;
		}

		Object limit = limitMd.getLimit();
		if (limit == null)
			return null;

		return (entity, p, value) -> {
			if (value == null)
				return null;

			int compareTo = ((Comparable<Object>) value).compareTo((Object) limit);
			boolean exclusive = limitMd.getExclusive();
			final boolean match;
			if (limitType == Min.T)
				match = exclusive ? compareTo > 0 : compareTo >= 0;
			else
				match = exclusive ? compareTo < 0 : compareTo <= 0;

			if (match)
				return null;

			final String val;
			if (propertyType == EssentialTypes.TYPE_STRING) {
				val = "'" + StringTools.truncateIfRequired((String) value, 100) + "'";
			} else {
				val = String.valueOf(value);
			}

			return "Value " + val + " does not match " + shortName + " limit " + limit + " (exclusive: " + exclusive
					+ ")";
		};
	}

	private InstancePropertyValidator validateMinLength(EntityType<?> entityType, Property property) {
		return validateLength(MinLength.T, entityType, property);
	}

	private InstancePropertyValidator validateMaxLength(EntityType<?> entityType, Property property) {
		return validateLength(MaxLength.T, entityType, property);
	}

	private InstancePropertyValidator validateLength(EntityType<? extends UniversalMetaData> mdType,
			EntityType<?> entityType, Property property) {
		UniversalMetaData lengthMd = cmdResolver.getMetaData().entityType(entityType).property(property).meta(mdType)
				.exclusive();
		if (lengthMd == null)
			return null;

		final boolean isMinLength;
		if (mdType == MinLength.T)
			isMinLength = true;
		else if (mdType == MaxLength.T)
			isMinLength = false;
		else
			return null;

		GenericModelType propertyType = property.getType();
		String shortName = mdType.getShortName();
		if (propertyType != EssentialTypes.TYPE_STRING) {
			// TODO: what to return here? error? log?
			logger.log(Level.WARNING,
					shortName + " metadata should only be applied on string-typed properties. Found on "
							+ entityType.getShortName() + "." + property.getName() + " of type "
							+ propertyType.getTypeSignature());
			// temporarily lenient error-handling
			return null;
		}

		return (entity, p, value) -> {
			if (value == null)
				return null;

			String strValue = (String) value;

			final boolean match;
			final long length;
			if (isMinLength) {
				length = ((MinLength) lengthMd).getLength();
				match = strValue.length() >= length;
			} else {
				length = ((MaxLength) lengthMd).getLength();
				match = strValue.length() <= length;
			}

			if (match)
				return null;

			return "Value '" + StringTools.truncateIfRequired(strValue, 100) + "' does not match " + shortName + " "
					+ length;
		};
	}

	private InstancePropertyValidator validateMandatory(EntityType<?> entityType, Property property) {
		boolean mandatory = cmdResolver.getMetaData().entityType(entityType).property(property).is(Mandatory.T);

		if (!mandatory)
			return null;

		return (entity, p, value) -> {
			if (value == null)
				return "Property is mandatory";
			else
				return null;
		};
	}

	public interface PropertyValidator {
		public InstancePropertyValidator validate(EntityType<?> entityType, Property property);
	}

	interface InstancePropertyValidator {
		String validate(GenericEntity entity, Property property, Object value);
	}
}
