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
package com.braintribe.model.openapi.v3_0;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#data-types
 * <p>
 * 
 * @see OpenapiFormat
 */
public enum OpenapiType implements EnumBase, StringRepresented {
	BOOLEAN("boolean"), //
	STRING("string"), //
	NUMBER("number"), //
	INTEGER("integer"), //
	OBJECT("object"), //
	ARRAY("array");

	public static final EnumType T = EnumTypes.T(OpenapiType.class);

	@Override
	public EnumType type() {
		return T;
	}

	String stringRepresentation;

	private static Map<String, OpenapiType> stringToFormat;

	public static OpenapiType parse(String formatString) {
		if (stringToFormat == null) {
			Map<String, OpenapiType> map = new HashMap<>();

			Stream.of(OpenapiType.values()).forEach(v -> map.put(v.stringRepresentation(), v));

			stringToFormat = map;
		}

		return stringToFormat.get(formatString);
	}

	private OpenapiType(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}

	@Override
	public String stringRepresentation() {
		return stringRepresentation;
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
