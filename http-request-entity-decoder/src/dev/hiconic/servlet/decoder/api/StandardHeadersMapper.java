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
package dev.hiconic.servlet.decoder.api;

import java.util.Collection;
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;

import dev.hiconic.servlet.decoder.impl.HttpRequestEntityDecoderUtils;
import dev.hiconic.servlet.decoder.impl.header.AbstractStandardHeadersMapper;
import dev.hiconic.servlet.decoder.impl.header.PropertyBasedStandardHeadersMapper;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * The interface for any class that can map header name to {@link Property}.
 * </p>
 * 
 * <p>
 * This interface also contains static methods to create commonly used mappers. For the following mappers, the header
 * names are {@link AbstractStandardHeadersMapper#getPropertyNameFromStandardHeaderName(String) transformed} before a
 * property trying to find a property with the corresponding name:
 * <ul>
 * <li>{@link #mapToProperties(EntityType)} map headers to all properties of the given entity type (properties' name
 * must match transformed header names)</li>
 * <li>{@link #mapToDeclaredProperties(EntityType)} map headers to declared properties of the given entity type
 * (properties' name must match transformed header names)</li>
 * <li>{@link #mapToProperties(Collection)} map headers to the given properties (properties' name must match transformed
 * header names)</li>
 * <li>{@link #mapToProperties(Map)} map headers properties with with the given mapping header name -> property (Map's
 * keys must match transformed header names)</li>
 * </ul>
 * </p>
 * 
 * @param <T>
 *            the entity type this mapper supports.
 */
public interface StandardHeadersMapper<T extends GenericEntity> {

	/**
	 * <p>
	 * Creates a mapper that maps standard headers to the declared properties of the given entity type.
	 * </p>
	 * 
	 * <p>
	 * For each header, looks up the property with the header's
	 * {@link AbstractStandardHeadersMapper#getPropertyNameFromStandardHeaderName(String) transformed name} in the
	 * entity type's declared properties.
	 * </p>
	 * 
	 * @param entityType
	 *            the entity type, must not be {@code null}
	 * @return the mapper, never {@code null}
	 */
	static <U extends GenericEntity> StandardHeadersMapper<U> mapToDeclaredProperties(EntityType<U> entityType) {
		return new PropertyBasedStandardHeadersMapper<>(entityType.getDeclaredProperties());
	}

	/**
	 * <p>
	 * Creates a mapper that maps standard headers to the all properties of the given entity type (including super
	 * types).
	 * </p>
	 * 
	 * <p>
	 * For each header, looks up the property with the header's
	 * {@link AbstractStandardHeadersMapper#getPropertyNameFromStandardHeaderName(String) transformed name} in the
	 * entity type's properties.
	 * </p>
	 * 
	 * @param entityType
	 *            the entity type, must not be {@code null}
	 * @return the mapper, never {@code null}
	 */
	static <U extends GenericEntity> StandardHeadersMapper<U> mapToProperties(EntityType<U> entityType) {
		return new PropertyBasedStandardHeadersMapper<>(HttpRequestEntityDecoderUtils.filterOutInvalidHeaderProperties(entityType.getProperties()));
	}

	/**
	 * <p>
	 * Creates a mapper that maps standard headers to properties in the given collection.
	 * </p>
	 * 
	 * <p>
	 * For each header, looks up the property with the header's
	 * {@link AbstractStandardHeadersMapper#getPropertyNameFromStandardHeaderName(String) transformed name} in the
	 * collection.
	 * </p>
	 * 
	 * @param properties
	 *            the collection of properties to look into, must not be {@code null}
	 * @return the mapper, never {@code null}
	 */
	static <U extends GenericEntity> StandardHeadersMapper<U> mapToProperties(Collection<Property> properties) {
		return new PropertyBasedStandardHeadersMapper<>(properties);
	}

	/**
	 * <p>
	 * Creates a mapper that maps standard headers to properties in the given map.
	 * </p>
	 * 
	 * <p>
	 * For each header, looks up the property with the header's
	 * {@link AbstractStandardHeadersMapper#getPropertyNameFromStandardHeaderName(String) transformed name} in the map.
	 * </p>
	 * 
	 * @param properties
	 *            the map of properties, by transformed header name, must not be {@code null}
	 * @return the mapper, never {@code null}
	 */
	static <U extends GenericEntity> StandardHeadersMapper<U> mapToProperties(Map<String, Property> properties) {
		return new PropertyBasedStandardHeadersMapper<>(properties);
	}

	/**
	 * <p>
	 * Called by the {@link HttpRequestEntityDecoder} do decode standard headers.
	 * </p>
	 * 
	 * <p>
	 * This method, given a request, header, and target entity, should:
	 * <ul>
	 * <li>Find the property that is mapped to the given header, if any</li>
	 * <li>If found, decode the value(s) of this header</li>
	 * <li>Assign the value(s) to the property</li>
	 * </ul>
	 * </p>
	 * 
	 * @param request
	 *            the request to decode, must not be {@code null}
	 * @param headerName
	 *            the <b>non transformed</b> name of the header to decode, must not be {@code null}
	 * @param target
	 *            the target fill, must not be {@code null}
	 */
	void assign(HttpServletRequest request, String headerName, T target);

}
