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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.tools.GmValueCodec;
import com.braintribe.model.meta.GmMetaModel;

/**
 * 
 * {@link GenericEntity}s encoded with this class can be decoded again with {@link QueryParamDecoder} which is used by our REST API.
 * 
 * @author Neidhart.Orlich
 *
 */
public class QueryParamEncoder {
	private StringBuilder stringBuilder;
	private int nextReferenceNumber = 0;
	private final Map<GenericEntity, String> references = new HashMap<>();
	
	public void registerTarget(String alias, GenericEntity entity) {
		references.put(entity, alias);
	}
	
	public String encode() {
		stringBuilder = null;
		nextReferenceNumber = 0;
		
		new HashMap<>(references).forEach((k,v) -> {
			encodeProperties(k, v);
		});
		references.clear();
		
		if (stringBuilder == null) {
			return "";
		}
		
		return stringBuilder.toString();
	}
	
	private void addParam(String key, String value) {
		if (stringBuilder == null) {
			stringBuilder = new StringBuilder();
		} else {
			stringBuilder.append('&');
		}
		stringBuilder.append(key + "=" + value);
	}
	
	private String nextAlias() {
		return Integer.toString(nextReferenceNumber++);
	}
	
	private void encodeEntity(EntityType<?> expectedType, GenericEntity entity, String key) {
		EntityType<GenericEntity> actualType = entity.entityType();
		
		String entityAlias = references.get(entity);
		if (entityAlias != null) {
			addParam(key, "@" + entityAlias);
			return;
		}
		
		String alias = nextAlias();
		references.put(entity, alias);

		if (!expectedType.equals(actualType)) {
			addParam("@" + alias, actualType.getTypeSignature());
		}
		
		addParam(key, "@" + alias);
		
		encodeProperties(entity, alias);
	}

	private void encodeProperties(GenericEntity entity, String entityAlias) {
		EntityType<?> actualType = entity.entityType();
		for (Property property: actualType.getProperties()) {
			Object propertyValue = property.get(entity);
			if (!property.isEmptyValue(propertyValue)) {
				encodeObject(property.getType(), propertyValue, entityAlias + "." + property.getName());
			}
		}
	}
	
	private void encodeLinearCollection(LinearCollectionType linearCollectionType, Collection<?> collection, String key) {
		collection.forEach(e -> encodeObject(linearCollectionType.getCollectionElementType(), e, key));
	}
	
	private void encodeMap(MapType mapType, Map<?,?> map, String key) {
		
		map.forEach((k,v) -> {
			String entryAlias = nextAlias();
			addParam(key, "@" + entryAlias);
			encodeObject(mapType.getKeyType(), k, entryAlias + ".key");
			encodeObject(mapType.getValueType(), v, entryAlias + ".value");
		});
	}
	
	private void encodeObject(GenericModelType expectedType, Object object, String key) {
		GenericModelType type = GMF.getTypeReflection().getType(object);
		
		switch (expectedType.getTypeCode()) {
			case stringType:
			case integerType:
			case booleanType:
			case enumType:
			case longType:
				addParam(key, object.toString());
				break;
			case decimalType:
			case doubleType:
			case floatType:
				addParam(key, GmValueCodec.objectToGmString(object));
				break;
			case dateType:
				addParam(key, HttpRequestEntityDecoderUtils.serializeDate((Date) object));
				break;
			case entityType:
				encodeEntity((EntityType<?>) expectedType, (GenericEntity) object, key);
				break;
			case mapType:
				encodeMap((MapType)type, (Map<?,?>)object, key);
				break;
			case objectType:
				if (type.isEntity()) {
					encodeEntity(GenericEntity.T, (GenericEntity) object, key);
				} else {
					addParam(key, GmValueCodec.objectToGmString(object));
				}
				break;
			case listType:
			case setType:
				encodeLinearCollection((LinearCollectionType) expectedType, (Collection<?>) object, key);
				break;
			default:
				throw new RuntimeException("QueryParamDecoder: Program should not have been able to enter this section.");
		}
	}
	
	public static void main(String[] args) {
		QueryParamEncoder encoder = new QueryParamEncoder();
		GmMetaModel model = GmMetaModel.T.create();
		model.setName("Model Name");
		encoder.registerTarget("model", model);
		
		String encoded = encoder.encode();
		
		System.out.println(encoded);
	}
}
