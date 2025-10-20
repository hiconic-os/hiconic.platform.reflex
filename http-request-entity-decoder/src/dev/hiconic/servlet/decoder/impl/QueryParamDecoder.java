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
package dev.hiconic.servlet.decoder.impl;

import static dev.hiconic.servlet.decoder.api.HttpExceptions.throwBadRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.braintribe.common.lcd.Pair;
import com.braintribe.exception.HttpException;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EnhancableCustomType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.model.generic.reflection.TypeCode;
import com.braintribe.model.generic.tools.GmModelTools;
import com.braintribe.model.generic.tools.GmValueCodec;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.CollectionTools;

import dev.hiconic.servlet.decoder.api.DecoderTargetRegistry;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A helper class for {@link HttpRequestEntityDecoderImpl} for complex query parameter decoding. 
 * 
 * @author Neidhart.Orlich
 */
public class QueryParamDecoder implements DecoderTargetRegistry {
	private final Map<String, Reference> references = new HashMap<>();
	private final Map<GenericModelType, Function<String, Object>> speciallyHandledTypes = new HashMap<>();
	private final List<String> targetAliases = new ArrayList<>();
	private final static char DOT = '.';
	private final static char REFERENCE_ANNOUNCEMENT = '@';
	private final static Set<String> invalidAliases = CollectionTools.getSet();
	
	private final HttpRequestEntityDecoderOptions entityDecoderOptions;

	public QueryParamDecoder(HttpRequestEntityDecoderOptions entityDecoderOptions) {
		this.entityDecoderOptions = entityDecoderOptions;
	}

	public void decode(String parameterString) {
		if (!StringTools.isBlank(parameterString)) {
			Stream<Pair<String, String>> parameters = Arrays.stream(parameterString.split("&")).map(p -> splitDelimitedPair(p, '='));

			decode(parameters);
		}

	}

	public void decode(Stream<Pair<String, String>> parameters) {
		parameters.forEach(p -> decode(p.first(), p.second()));
	}

	public void decode(Iterable<Pair<String, String>> parameters) {
		parameters.forEach(p -> decode(p.first(), p.second()));
	}

	public void decode(String key, String value) {
		try {
			_decode(key, value);
		} catch (HttpException e) {
			throw new HttpException(HttpServletResponse.SC_BAD_REQUEST, "Error while parsing query parameter '" + key + "=" + value + "'.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Error while parsing query parameter '" + key + "=" + value + "'.", e);
		}
	}
	
	/**
	 * Splits the passed <code>string</code> once, i.e. at the last occurrence of the <code>delimiter</code>, which is
	 * NOT handled as a regular expression. If the delimiter is not found a {@link Pair} with a nulled {@link Pair#second()} is returned.
	 */
	public static Pair<String, String> splitDelimitedPair(final String string, final char delimiter) throws IllegalArgumentException {
		if (string == null) {
			throw new IllegalArgumentException("Argument string must not be null");
		}
		
		final int index = string.lastIndexOf(delimiter);
		if (index < 0) {
			return Pair.of(string, null);
		}
		else {
			final String first = string.substring(0, index);
			final String second = string.substring(index + 1);
			return Pair.of(first, second);
		}
	}
	
	private void _decode(String key, String value) {
		int lastDot = key.lastIndexOf(DOT);

		if (lastDot == -1) {
			if (isAlias(key)) {
				String alias = key.substring(1);
				set(alias, null, value);
			} else {
				setPropertyOfRootReference(key, value);
			}
		} else {
			Pair<String, String> splitDelimitedPair = splitDelimitedPair(key, DOT);
			String alias = splitDelimitedPair.first();

			if (invalidAliases.contains(alias)) {
				badQueryParam("'" + alias + "' is no allowed reference name.");
			}

			String propertyName = splitDelimitedPair.second();
			set(alias, propertyName, value);
		}
	}

	private void setPropertyOfRootReference(String propertyName, String value) {
		setPropertyOfRootReference(propertyName, value, true);
	}
	
	private void setPropertyOfRootReference(String propertyName, String value, boolean sanitizeWhenNotFound) {
		for (String alias : targetAliases) {
			Reference reference = references.get(alias);

			if (reference.type.isEntity()) {
				EntityType<?> referencedType = (EntityType<?>) reference.type;
				Property referencedProperty = referencedType.findProperty(propertyName);

				if (referencedProperty != null) {
					set(alias, propertyName, value);
					return;
				}
			}
		}

		if (sanitizeWhenNotFound) {
			String sanitizedPropertyName = HttpRequestEntityDecoderImpl.hyphenToCamelCase(propertyName);
			setPropertyOfRootReference(sanitizedPropertyName, value, false);
		}
		else if (!entityDecoderOptions.isIgnoringUnmappedUrlParameters()) {
			badQueryParam("Neither of the following targets " + targetAliases + " have a property '" + propertyName + "'.");
		}
	}

	private void set(String alias, String propertyName, String value) {
		if (propertyName == null) {
			if (entityDecoderOptions.getIgnoredParameters().contains(alias)) {
				badQueryParam("Reference '" + alias + "' was already defined and is being ignored. Please use another name.");
			}
			
			if (references.containsKey(alias)) {
				badQueryParam("Reference '" + alias + "' was already defined. Please set its type upon definition (upon first mentioning).");
			}
			EntityType<?> entityType = GMF.getTypeReflection().getEntityType(value);
			registerNew(alias, entityType);
		} else {
			if (entityDecoderOptions.getIgnoredParameters().contains(alias)) {
				return;
			}
			
			Reference reference = references.get(alias);

			if (reference == null) {
				if (entityDecoderOptions.isIgnoringUnmappedUrlParameters())
					return;
				
				badQueryParam("You must declare alias '" + alias + "' before you can use it");
			}

			GenericModelType type = reference.type;
			switch (type.getTypeCode()) {
				case entityType:
					EntityType<?> entityType = (EntityType<?>) type;
					Property property = entityType.getProperty(propertyName);
					GenericModelType valueType = property.getType();
					GenericEntity entity = (GenericEntity) reference.value;

					if (valueType instanceof LinearCollectionType) {
						LinearCollectionType collectionType = (LinearCollectionType) valueType;
						Collection<Object> collection = ensureInitialized(property, entity, collectionType);
						GenericModelType collectionElementType = collectionType.getCollectionElementType();
						Object elementValue = getValue(value, collectionElementType);
						collection.add(elementValue);
					} else if (valueType instanceof MapType) {
						MapType mapType = (MapType) valueType;
						
						Map<Object, Object> map = ensureInitialized(property, entity, mapType);
						MutableEntry entry = (MutableEntry) getValue(value, mapType);
						entry.applyToMap(map);
					} else {
						Object actualValue = getValue(value, valueType);
						property.set(entity, actualValue);
					}
					reference.onSet.run();
					break;
				case mapType:
					MutableEntry entry = (MutableEntry) reference.value;
					MapType mapType = (MapType) reference.type;
					if (propertyName.equals("key")) {
						entry.setKey(getValue(value, mapType.getKeyType()));
					} else if (propertyName.equals("value")) {
						entry.setValue(getValue(value, mapType.getValueType()));
					} else {
						badQueryParam("Alias '" + alias + "' references to a map entry of type '" + mapType + "' and has no property '" + propertyName
								+ "'.");
					}
					break;
				default:
					throw new IllegalStateException("Found alias of type '" + type + "' but only entity or map types are supported.");
			}
			
		}
	}

	private <T> T ensureInitialized(Property property, GenericEntity entity, EnhancableCustomType valueType) {
		Object value = property.get(entity);
		if (value == null) {  // Happens when property is set to absent
			value =  valueType.createPlain();
			property.set(entity, value);
		}
		return (T) value;
	}

	private Object getObjectValue(String value) {
		if (isAlias(value)) {
			String alias = value.substring(1);
			if (references.containsKey(alias))
				return references.get(alias).value;

			badQueryParam("Object value '" + alias + "' looks like an alias but was not declared yet.");
		}

		try {
			return GmValueCodec.objectFromGmString(value);
		} catch (IllegalArgumentException e) {
			// When value is no valid GmString we return it as the value
			return value;
		}
	}
	
	private Object handleNull(GenericModelType valueType) {
		if (valueType.getTypeCode() == TypeCode.objectType && !entityDecoderOptions.isNullAware())
			return "null";
		else
			return null;
	}

	private Object getValue(String value, GenericModelType valueType) {
		if ("null".equals(value)) {
			return handleNull(valueType);
		}
		
		Function<String, Object> specialHandling = speciallyHandledTypes.get(valueType);
		if (specialHandling != null) {
			return specialHandling.apply(value);
		}

		switch (valueType.getTypeCode()) {
			case stringType:
			case integerType:
			case booleanType:
			case decimalType:
			case doubleType:
			case enumType:
			case floatType:
			case longType:
				ScalarType scalarType = (ScalarType) valueType;
				return scalarType.instanceFromString(value);
			case dateType:
				return HttpRequestEntityDecoderUtils.parseDate(value);
			case entityType:
			case mapType:
				assertIsValidAlias(value);
				return getReference(value.substring(1), valueType).value;
			case objectType:
				return getObjectValue(value);
			case listType:
			case setType:
			default:
				throw new RuntimeException("QueryParamDecoder: Program should not have been able to enter this section.");
		}
	}

	private void assertIsValidAlias(String value) {
		if (!isAlias(value)) {
			badQueryParam("Expected reference but got '" + value + "'.");
		}
	}

	private Reference getReference(String alias, GenericModelType type) {
		Reference reference = references.get(alias);
		
		if (reference == null) {
			alias = HttpRequestEntityDecoderImpl.hyphenToCamelCase(alias);
			reference = references.get(alias);
		}
		
		if (reference == null) {
			reference = registerNew(alias, type);
		} else if (!type.isAssignableFrom(reference.type)) {
			badQueryParam("Expected to resolve reference of type '" + type.getTypeSignature() + "' but reference '" + alias + "' has type '"
					+ reference.type + "'.");
		}
		return reference;
	}

	private boolean isAlias(String alias) {
		return !StringTools.isBlank(alias) && alias.charAt(0) == REFERENCE_ANNOUNCEMENT;
	}

	public void registerTarget(String alias, GenericEntity value) {
		registerTarget(alias, value, () ->{/*noop*/});
	}
	
	public void registerTarget(String alias, GenericEntity value, Runnable onSet) {
		if (references.containsKey(alias)) {
			throw new IllegalStateException("Can't create reference '" + alias + "' as it already exists.");
		}

		Reference reference = new Reference(value, value.entityType(), onSet);

		references.put(alias, reference);
		targetAliases.add(alias);
	}

	private Reference registerNew(String alias, GenericModelType type) {
		if (references.containsKey(alias)) {
			badQueryParam("Can't create reference '" + alias + "' as it already exists.");
		}

		Object value = null;
		if (type.isEntity()) {
			EntityType<?> entityType = (EntityType<?>) type;
			// Setting properties to absent so that they can later be lazy-loaded by AccessRequestProcessors
			value = GmModelTools.createShallow(entityType);
		} else if (type instanceof MapType) {
			value = new MutableEntry(alias);
		} else {
			badQueryParam("Alias type of " + type + " not allowed.");
		}

		Reference reference = new Reference(value, type);

		references.put(alias, reference);

		return reference;
	}

	private static class Reference {
		public final Object value;
		public final GenericModelType type;
		public final Runnable onSet;

		public Reference(Object value, GenericModelType type) {
			this(value, type, () -> {/*noop*/});
		}
		
		public Reference(Object value, GenericModelType type, Runnable onSet) {
			super();
			this.value = value;
			this.type = type;
			this.onSet = onSet;
		}
	}

	private static class MutableEntry {
		private static Object notSet = new Object();

		public Object key = notSet;
		public Object value = notSet;
		private Map<Object, Object> map;
		private final String alias;

		public MutableEntry(String alias) {
			this.alias = alias;
		}

		public boolean initialized() {
			return key != notSet && value != notSet && map != null;
		}

		private void updateMap() {
			if (initialized()) {
				map.put(key, value);
				map = null;
			}
		}

		public void setKey(Object key) {
			if (this.key != notSet) {
				badQueryParam("Error while creating map entry '" + alias + "'. Map key set multiple times for the same entry: " + key);
			}
			this.key = key;
			updateMap();
		}

		public void setValue(Object value) {
			if (this.value != notSet) {
				badQueryParam("Error while creating map entry '" + alias + "'. Map value set multiple times for the same entry: " + key);
			}
			this.value = value;
			updateMap();
		}

		public void applyToMap(Map<Object, Object> map) {
			if (this.map != null) {
				badQueryParam("Error while creating map entry '" + alias + "'. Please set key and value before applying reference to another map.");
			}
			this.map = map;
			updateMap();
		}

	}

	private static void badQueryParam(String message) {
		throwBadRequest("Could not parse query parameters: " + message);
	}
	
	public void addSpecialHandlingFor(GenericModelType type, Function<String,Object> handling) {
		speciallyHandledTypes.put(type, handling);
	}

	@Override
	public DecoderTargetRegistry target(String prefix, GenericEntity target, Runnable onSet) {
		registerTarget(prefix, target, onSet);
		return this;
	}
}
