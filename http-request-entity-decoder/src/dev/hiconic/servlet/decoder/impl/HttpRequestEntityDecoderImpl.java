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
package dev.hiconic.servlet.decoder.impl;

import static dev.hiconic.servlet.decoder.api.HttpExceptions.throwBadRequest;
import static dev.hiconic.servlet.decoder.impl.HttpRequestEntityDecoderUtils.checkArgumentNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.common.lcd.Pair;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.CollectionType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.Property;

import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoder;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;
import dev.hiconic.servlet.decoder.api.StandardHeadersMapper;
import jakarta.servlet.http.HttpServletRequest;

public class HttpRequestEntityDecoderImpl implements HttpRequestEntityDecoder {

	private static final Logger logger = Logger.getLogger(HttpRequestEntityDecoderImpl.class);

	private static final String PREFIX_DELIMITER = ".";

	private static final String HEADER_GM_PREFIX = "gm-";

	private final HttpServletRequest httpRequest;

	private final HttpRequestEntityDecoderOptions options;

	private final List<DecoderTarget> targets = new ArrayList<>();

	private boolean decodeInvoked = false;

	public HttpRequestEntityDecoderImpl(HttpServletRequest httpRequest, HttpRequestEntityDecoderOptions options) {
		this.httpRequest = httpRequest;
		this.options = options;
	}

	@Override
	public HttpRequestEntityDecoder target(String prefix, GenericEntity target) {
		addTarget(prefix, target);
		return this;
	}
	
	@Override
	public HttpRequestEntityDecoder target(String prefix, GenericEntity target, Runnable onSet) {
		DecoderTarget decoderTarget = addTarget(prefix, target);
		decoderTarget.onSet(onSet);
		return this;
	}

	@Override
	public <T extends GenericEntity> HttpRequestEntityDecoder target(String prefix, T target, StandardHeadersMapper<? super T> headerPropertyMapper) {
		DecoderTarget decoderTarget = addTarget(prefix, target);
		decoderTarget.standardHeadersMapper = (StandardHeadersMapper<GenericEntity>) headerPropertyMapper;
		return this;
	}

	private <T extends GenericEntity> DecoderTarget addTarget(String prefix, T target) {
		checkArgumentNotNull(prefix, "prefix");
		checkArgumentNotNull(target, "target");
		DecoderTarget decoderTarget = new DecoderTarget(prefix, target);
		decoderTarget.onSet = () -> {/*noop*/};
		targets.add(decoderTarget);
		
		return decoderTarget;
	}

	@Override
	public void decode() {
		checkDecodeNotAlreadyInvoked();
		checkTargetsNotEmpty();

		decodeUrlParameters();

		if (!options.isIgnoringHeaders())
			decodeHeaders();
	}

	private void decodeUrlParameters() {
		QueryParamDecoder queryParamDecoder = new QueryParamDecoder(options);

		targets.forEach(target -> queryParamDecoder.registerTarget(target.getPrefix(), target.getEntity(), target.onSet));

		Stream<Pair<String, String>> paramStream = httpRequest.getParameterMap().entrySet() //
				.stream() //
				.flatMap(e -> //
					Arrays.stream(e.getValue()) //
							.map(v -> Pair.of(e.getKey(), v)) //
				);

		queryParamDecoder.decode(paramStream);
	}

	private void decodeHeaders() {
		Enumeration<String> headerNames = httpRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String header = headerNames.nextElement();

			if (isGenericHeader(header)) {
				DecoderTarget target = findTargetForParameter(header, true);

				if (target == null) {
					continue;
				}

				Enumeration<String> headerValues = httpRequest.getHeaders(header);
				while (headerValues.hasMoreElements()) {
					setValue(target, headerValues.nextElement());
				}
			} else {
				for (DecoderTarget target : targets) {
					GenericEntity entity = target.getEntity();
					StandardHeadersMapper<GenericEntity> mapper = target.getStandardHeadersMapper();
					if (mapper != null) {
						mapper.assign(httpRequest, header, entity);
					}
				}
			}
		}
	}

	private DecoderTarget findTargetForParameter(String parameter, boolean isHeader) {
		final String sanitizedParameter = sanitize(parameter, isHeader);

		DecoderTarget target = null;
		if (hasPrefix(sanitizedParameter)) {
			target = findTargetForParameterWithPrefix(sanitizedParameter, parameter, isHeader);
		} else {
			target = findTargetForParameterWithoutPrefix(sanitizedParameter, parameter, isHeader);
		}

		if (target == null) {
			if (isHeader && !options.isIgnoringUnmappedHeaders()) {
				throwBadRequest("No property found for header parameter: %s", parameter);
			}
			if (!isHeader && !options.isIgnoringUnmappedUrlParameters()) {
				throwBadRequest("No property found for URL parameter: %s", parameter);
			}
		}

		return target;
	}

	private DecoderTarget findTargetForParameterWithPrefix(String sanitizedParameter, String parameter, boolean isHeader) {
		int lastIndexOfDot = sanitizedParameter.lastIndexOf('.');

		String prefix = sanitizedParameter.substring(0, lastIndexOfDot);
		DecoderTarget target = getTargetByPrefix(prefix);
		if (target == null) {
			if ((isHeader && options.isIgnoringUnmappedHeaders()) || (!isHeader && options.isIgnoringUnmappedUrlParameters())) {
				return null;
			}
			throwBadRequest("No prefix registered with name \"%s\", invalid %s parameter: %s", prefix, isHeader ? "header" : "URL", parameter);
		}

		String propertyName = sanitizedParameter.substring(lastIndexOfDot + 1);
		EntityType<GenericEntity> entityType = target.getEntity().entityType();
		Property property = entityType.findProperty(propertyName);
		if (property == null) {
			// badRequest("No property found with name \"%s\", invalid %s parameter: %s", propertyName, isHeader ?
			// "header" : "URL", parameter);
			return null;
		}

		target.setCurrentlyTargetedProperty(property, isHeader, parameter);
		return target;
	}

	private DecoderTarget findTargetForParameterWithoutPrefix(String sanitizedParameter, String parameter, boolean isHeader) {
		for (DecoderTarget target : this.targets) {
			EntityType<GenericEntity> entityType = target.getEntity().entityType();
			Property property = entityType.findProperty(sanitizedParameter);
			if (property == null) {
				continue;
			}
			target.setCurrentlyTargetedProperty(property, isHeader, parameter);
			return target;
		}

		// badRequest("No property found with name \"%s\", invalid %s parameter: %s", sanitizedParameter, isHeader ?
		// "header" : "URL", parameter);
		return null;
	}

	private void setValue(DecoderTarget target, String value) {
		if (target == null) {
			return;
		}
		GenericEntity entity = target.getEntity();
		Property property = target.getTargetedProperty();
		GenericModelType type = property.getType();
		if (!type.isCollection() && target.isTargetPropertyValueSet()) {
			throwBadRequest("Multiple values found in %s parameter %s mapped to property %s of entity %s but the property is not a collection.",
					target.isPropertyTargetedFromHeader() ? "header" : "URL", target.getTargetedParameter(), property.getName(),
					entity.entityType().getTypeSignature());
		}

		ensureInitialized(entity, property);
		addOrSetValue(target, value);
	}

	private Object decodeScalarValue(DecoderTarget target, GenericModelType type, String encodedValue) {
		try {
			switch (type.getTypeCode()) {
				case booleanType:
					return Boolean.parseBoolean(encodedValue);
				case dateType:
					return HttpRequestEntityDecoderUtils.parseDate(encodedValue);
				case decimalType:
					return new BigDecimal(encodedValue);
				case doubleType:
					return Double.parseDouble(encodedValue);
				case stringType:
					return encodedValue;
				case floatType:
					return Float.parseFloat(encodedValue);
				case integerType:
					return Integer.parseInt(encodedValue);
				case longType:
					return Long.parseLong(encodedValue);
				case enumType:
					return ((EnumType<?>) type).getEnumValue(encodedValue);
				default:
					throwBadRequest("type is not supported as scalar (non collection): %s", type.getTypeSignature());
					return null;
			}
		} catch (IllegalArgumentException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error while parsing value", e);
			}
			throwBadRequest("Cannot parse %s parameter %s. Expected type %s but got \"%s\"", target.isPropertyTargetedFromHeader() ? "header" : "URL",
					target.getTargetedParameter(), type.getTypeName(), encodedValue);
			return null;
		}
	}

	private void addOrSetValue(DecoderTarget target, String value) {
		Property property = target.getTargetedProperty();
		GenericEntity entity = target.getEntity();
		Object appliedValue = null;
		switch (property.getType().getTypeCode()) {
			case listType:
			case setType:
				Collection<Object> collection = property.get(entity);
				CollectionType type = (CollectionType) property.getType();
				appliedValue = decodeScalarValue(target, type.getCollectionElementType(), value);
				collection.add(appliedValue);
				break;
			case mapType:
				throwBadRequest("Map type properties are not allowed in headers or URL parameters, Cannot parse property %s for entity type %s",
						property.getName(), entity.entityType().getTypeSignature());
				break;
			case entityType:
				throwBadRequest("Entity type properties are not allowed in headers or URL parameters, Cannot parse property %s for entity type %s",
						property.getName(), entity.entityType().getTypeSignature());
				break;
			case objectType:
				throwBadRequest("Object type properties are not allowed in headers or URL parameters, Cannot parse property %s for entity type %s",
						property.getName(), entity.entityType().getTypeSignature());
				break;
			default:
				appliedValue = decodeScalarValue(target, property.getType(), value);
				property.set(entity, appliedValue);
				break;
		}
		
		target.onSet.run();
	}

	private DecoderTarget getTargetByPrefix(String prefix) {
		for (DecoderTarget target : targets) {
			if (prefix.equals(target.getPrefix())) {
				return target;
			}
		}

		return null;
	}

	public static String sanitize(String parameter, boolean isHeader) {
		if (isHeader) {
			String noHeaderPrefix = parameter.substring(HEADER_GM_PREFIX.length());
			return hyphenToCamelCase(noHeaderPrefix);
		}
		return hyphenToCamelCase(parameter);
	}

	public static String hyphenToCamelCase(String name) {
		if (name != null & name.contains("-")) {
			String camelCased = Arrays.stream(name.split("\\-")).map(String::toLowerCase).map(s -> {
				if (s.length() > 1) {
					return s.substring(0, 1).toUpperCase() + s.substring(1);
				}
				return s;
			}).collect(Collectors.joining());
			return camelCased.substring(0, 1).toLowerCase() + camelCased.substring(1);
		}
		return name;
	}

	private void ensureInitialized(GenericEntity entity, Property property) {
		if (property.get(entity) != null) {
			return;
		}

		switch (property.getType().getTypeCode()) {
			case setType:
			case listType:
				LinearCollectionType collectionType = property.getType().cast();
				property.set(entity, collectionType.createPlain());
				break;
			case mapType:
				throwBadRequest("Map type properties are not allowed in headers or URL parameters.");
				break;
			default:
		}
	}

	private boolean hasPrefix(String parameter) {
		return parameter.contains(PREFIX_DELIMITER);
	}

	private boolean isGenericHeader(String parameter) {
		return parameter.startsWith(HEADER_GM_PREFIX);
	}

	private void checkDecodeNotAlreadyInvoked() {
		if (decodeInvoked) {
			throw new IllegalStateException("This decoder has already been used once. Please create one decoder per request.");
		}
		decodeInvoked = true;
	}

	private void checkTargetsNotEmpty() {
		if (targets.isEmpty()) {
			throw new IllegalStateException("Please register at least one target entity to decode into with the target(...) methods.");
		}
	}

	private static class DecoderTarget {

		private final GenericEntity entity;

		private final String prefix;

		private StandardHeadersMapper<GenericEntity> standardHeadersMapper;

		private Property targetedProperty;

		private String targetedParameter;

		private boolean targetPropertyValueSet;

		private boolean propertyTargetedFromHeader;
		
		private Runnable onSet;

		public DecoderTarget(String prefix, GenericEntity entity) {
			this.prefix = prefix;
			this.entity = entity;
		}

		public GenericEntity getEntity() {
			return entity;
		}

		public StandardHeadersMapper<GenericEntity> getStandardHeadersMapper() {
			return standardHeadersMapper;
		}

		public void onSet(Runnable onSet) {
			this.onSet = onSet;
		}
		
		public String getPrefix() {
			return prefix;
		}

		public Property getTargetedProperty() {
			return targetedProperty;
		}

		public String getTargetedParameter() {
			return targetedParameter;
		}

		public void setCurrentlyTargetedProperty(Property targetedProperty, boolean fromHeader, String parameterName) {
			this.targetedProperty = targetedProperty;
			this.targetedParameter = parameterName;
			this.targetPropertyValueSet = false;
			this.propertyTargetedFromHeader = fromHeader;
		}

		public boolean isTargetPropertyValueSet() {
			return targetPropertyValueSet;
		}

		public boolean isPropertyTargetedFromHeader() {
			return propertyTargetedFromHeader;
		}
	}
}
