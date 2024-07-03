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
package hiconic.rx.web.ddra.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import com.braintribe.codec.marshaller.api.CharacterMarshaller;
import com.braintribe.codec.marshaller.api.CharsetOption;
import com.braintribe.codec.marshaller.api.DateFormatOption;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.GmMarshallingOptions;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.MarshallException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.common.lcd.Pair;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.CollectionType;
import com.braintribe.model.generic.reflection.DateType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.GenericModelTypeReflection;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.collection.api.ListMap;
import com.braintribe.utils.collection.impl.HashListMap;

/**
 * A {@link Marshaller} implementation that takes a GenericModelValue and encodes it to a URL Query string <br />
 * <br />
 * 
 * @author gunther.schenk
 */
public class UrlEncodingMarshaller implements CharacterMarshaller {

	public static final String DEFAULT_CHARSET = "UTF-8";
	private static final DateTimeFormatter DEFAULT_DATEFORMATTER = DateTools.ISO8601_DATE_WITH_MS_FORMAT;

	public static final String URL_PARAM_SEPARATOR = "&";

	private static final Logger logger = Logger.getLogger(UrlEncodingMarshaller.class);
	private static final GenericModelTypeReflection typeReflection = GMF.getTypeReflection();

	private final EntityTemplateFactory rootEntityTemplateFactory;

	@FunctionalInterface
	public static interface EntityTemplateFactory {
		GenericEntity supply(ListMap<String, String> source);
	}

	public UrlEncodingMarshaller() {
		this(null);
	}

	public UrlEncodingMarshaller(EntityTemplateFactory rootEntityTemplateFactory) {
		this.rootEntityTemplateFactory = rootEntityTemplateFactory;
	}

	// ***************************************************************************************************
	// Marshaller
	// ***************************************************************************************************

	@Override
	public void marshall(Writer writer, Object value) throws MarshallException {
		marshall(writer, value, null);
	}

	@Override
	public void marshall(Writer writer, Object value, GmSerializationOptions options) throws MarshallException {
		String encodedValue = encode(null, value, options);
		try {
			writer.write(encodedValue);
		} catch (IOException e) {
			throw new MarshallException("Could not URL encode value: " + value, e);
		}

	}

	@Override
	public void marshall(OutputStream out, Object value) throws MarshallException {
		marshall(out, value, null);
	}

	@Override
	public void marshall(OutputStream out, Object value, GmSerializationOptions options) throws MarshallException {
		String encodedValue = encode(null, value, options);

		try {
			out.write(encodedValue.getBytes());
		} catch (IOException e) {
			throw new MarshallException("Could not URL encode value: " + value, e);
		}
	}

	@Override
	public Object unmarshall(InputStream in) throws MarshallException {
		return unmarshall(in, null);
	}

	@Override
	public Object unmarshall(InputStream in, GmDeserializationOptions options) throws MarshallException {
		try {
			// TODO: Implement streamingwise unmarshalling
			return unmarshall(IOTools.slurp(in, getCharacterSet(options)), options);
		} catch (IOException e) {
			return new MarshallException(e);
		}
	}

	@Override
	public Object unmarshall(Reader reader, GmDeserializationOptions options) throws MarshallException {
		try {
			// TODO: Implement streamingwise unmarshalling
			return unmarshall(IOTools.slurp(reader), options);
		} catch (IOException e) {
			return new MarshallException(e);
		}
	}

	public Object unmarshall(String string, GmDeserializationOptions options) {
		StringTokenizer stringTokenizer = new StringTokenizer(string, URL_PARAM_SEPARATOR);
		ListMap<String, String> params = new HashListMap<>();

		while (stringTokenizer.hasMoreTokens()) {
			String param = stringTokenizer.nextToken();
			Pair<String, String> paramPair = StringTools.splitDelimitedPair(param, '=');
			try {
				String key = URLDecoder.decode(paramPair.first(), getCharacterSet(options));
				String value = URLDecoder.decode(paramPair.second(), getCharacterSet(options));

				params.putSingleElement(key, value);
			} catch (UnsupportedEncodingException e) {
				throw new MarshallException("Unsupported encoding specified in marshaller options or as default.", e);
			}
		}

		GenericModelType inferredRootType = EssentialTypes.TYPE_MAP;

		if (options != null && options.getInferredRootType() != null) {
			inferredRootType = options.getInferredRootType();
		}

		return create(null, params, inferredRootType, options);
	}
	private Object createMap(String sourceKey, ListMap<String, String> source, MapType type, GmDeserializationOptions options) {
		GenericModelType keyType = type.getKeyType();
		GenericModelType valueType = type.getValueType();

		if (!(keyType instanceof ScalarType)) {
			throw new MarshallException("Maps with non-scalar key types not supported. Found key type: " + keyType);
		}

		Map<Object, Object> map = new HashMap<>();

		String mapKeyRegex;
		String mapKeyPrefix;
		if (sourceKey == null) {
			mapKeyRegex = "$\\w+";
			mapKeyPrefix = "";
		} else {
			mapKeyRegex = "$" + sourceKey + "\\\\.\\w+";
			mapKeyPrefix = sourceKey + ".";
		}

		source.keySet().stream() //
				.filter(k -> k.matches(mapKeyRegex)).forEach(k -> {
					Object key = createScalar(k, (ScalarType) keyType, options);
					Object value = create(source.getSingleElement(mapKeyPrefix + k), source, valueType, options);
					map.put(key, value);
				});

		return map;
	}

	private GenericEntity createEntity(String sourceKey, ListMap<String, String> source, EntityType<?> type, GmDeserializationOptions options) {
		GenericEntity entity;

		if (sourceKey == null && rootEntityTemplateFactory != null) {
			entity = rootEntityTemplateFactory.supply(source);
		} else {
			entity = type.create();
		}

		Boolean hasChildEntity = null;
		for (Property property : type.getProperties()) {
			String name = property.getName();
			String propertyKey = sourceKey == null ? name : sourceKey + "." + name;

			if (property.getType().isEntity()) {
				if (hasChildEntity == null) {
					hasChildEntity = source.keySet().stream().anyMatch(k -> k.startsWith(propertyKey));
				}
				if (hasChildEntity) {
					GenericEntity childEntity = createEntity(propertyKey, source, (EntityType<?>) property.getType(), options);
					if (childEntity != null) {
						property.set(entity, childEntity);
					}
				}
			} else if (source.containsKey(propertyKey)) {
				Object actualValue = create(propertyKey, source, property.getType(), options);
				property.set(entity, actualValue);
			}
		}
		return entity;
	}

	private Object createScalar(String value, ScalarType type, GmDeserializationOptions options) {
		if (value == null)
			return null;

		if (type instanceof DateType) {
			return decodeDate(value, options);
		}

		return type.instanceFromString(value);
	}

	public <T extends GenericEntity> T create(ListMap<String, String> source, EntityType<T> type, GmDeserializationOptions options) {
		return (T) create(null, source, type, options);
	}

	public Object create(String parentKey, ListMap<String, String> source, GenericModelType type, GmDeserializationOptions options) {
		if (type.isScalar())
			return createScalar(source.getSingleElement(parentKey), (ScalarType) type, options);
		if (type instanceof MapType)
			return createMap(parentKey, source, (MapType) type, options);
		if (type.isCollection())
			return createCollection(parentKey, source, (CollectionType) type, options);
		if (type.isEntity())
			return createEntity(parentKey, source, (EntityType<?>) type, options);

		throw new MarshallException("Unmarshalling values of type '" + type.getTypeSignature() + "' not supported.");
	}

	private Object createCollection(String parentKey, ListMap<String, String> source, CollectionType type, GmDeserializationOptions options) {
		Collection<String> elements = source.get(parentKey);
		Collection<Object> collection = (Collection<Object>) type.createPlain();

		if (!type.getCollectionElementType().isScalar()) {
			throw new MarshallException(
					"Unmarshalling collection with non-scalar type not suported. Found type: " + type.getCollectionElementType().getTypeSignature());
		}

		ScalarType collectionElementType = (ScalarType) type.getCollectionElementType();

		elements.stream().map(v -> createScalar(v, collectionElementType, options)).forEach(collection::add);
		return collection;
	}

	// ***************************************************************************************************
	// Encoding Helper
	// ***************************************************************************************************

	@SuppressWarnings("incomplete-switch")
	private String encode(String key, Object value, GmSerializationOptions options) {
		if (value == null) {
			return null;
		}

		GenericModelType type = typeReflection.getType(value);
		switch (type.getTypeCode()) {
			case booleanType:
			case doubleType:
			case integerType:
			case longType:
			case floatType:
			case decimalType:
			case stringType:
			case enumType:
				return encodeValue(key, value.toString(), options);
			case dateType:
				return encodeDate(key, value, options);
			case listType:
			case setType:
				return encodeCollection(key, (Collection<?>) value, options);
			case mapType:
				return encodeMap(key, (Map<?, ?>) value, options);
			case entityType:
				return encodeEntity(key, (EntityType<?>) type, (GenericEntity) value, options);
		}

		logger.warn("Unsupported value: " + value + " for key: " + key);
		return null;
	}

	private String encodeDate(String key, Object value, GmMarshallingOptions options) {
		DateTimeFormatter dateFormatter = getDateFormat(options);
		return encodeValue(key, DateTools.encode((Date) value, dateFormatter), options);
	}

	private Date decodeDate(String dateString, GmMarshallingOptions options) {
		DateTimeFormatter dateFormatter = getDateFormat(options);
		return DateTools.decodeDateTime(dateString, dateFormatter);
	}

	private String encodeValue(String key, String encodedValue, GmMarshallingOptions options) {
		String characterSet = getCharacterSet(options);
		String result = escape(encodedValue, characterSet);
		if (key != null) {
			result = escape(key, characterSet) + "=" + result;
		}
		return result;
	}

	private String escape(String value, String characterSet) {
		try {
			return URLEncoder.encode(value, characterSet);
		} catch (UnsupportedEncodingException e) {
			logger.warn("Unable to encode value: " + value + " with character set: " + characterSet);
		}
		return value;
	}

	private String getCharacterSet(GmMarshallingOptions options) {
		String charset = DEFAULT_CHARSET;
		if (options != null) {
			charset = options.findOrNull(CharsetOption.class);
		}
		return charset != null ? charset : DEFAULT_CHARSET;
	}

	private DateTimeFormatter getDateFormat(GmMarshallingOptions options) {
		DateTimeFormatter dateFormatter = DEFAULT_DATEFORMATTER;
		if (options != null) {
			String dateFormat = options.findOrNull(DateFormatOption.class);
			if (dateFormat != null) {
				dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
			}
		}
		return dateFormatter;
	}

	private String encodeEntity(String parentKey, EntityType<?> entityType, GenericEntity entity, GmSerializationOptions options) {
		Collection<String> encodedEntity = entityType.getProperties().stream()
				.map(p -> this.encode(encodeKey(parentKey, p.getName()), p.get(entity), options)).filter(Objects::nonNull)
				.collect(Collectors.toList());
		return encodeCollectionElements(encodedEntity);
	}

	private String encodeMap(String parentKey, Map<?, ?> map, GmSerializationOptions options) {
		Collection<String> encodedMap = map.entrySet().stream()
				.map(e -> this.encode(encodeKey(parentKey, e.getKey().toString()), e.getValue(), options)).filter(Objects::nonNull)
				.collect(Collectors.toList());
		return encodeCollectionElements(encodedMap);
	}

	private String encodeCollection(String key, Collection<?> collection, GmSerializationOptions options) {
		Collection<String> encodedCollection = collection.stream().map(e -> this.encode(key, e, options)).filter(Objects::nonNull)
				.collect(Collectors.toList());
		return encodeCollectionElements(encodedCollection);
	}

	private String encodeCollectionElements(Collection<String> encodedCollectionElements) {
		if (encodedCollectionElements.isEmpty()) {
			return null;
		}
		return StringTools.join(URL_PARAM_SEPARATOR, encodedCollectionElements);
	}

	private String encodeKey(String parentKey, String key) {
		return (parentKey != null) ? parentKey + "." + key : key;
	}

}
