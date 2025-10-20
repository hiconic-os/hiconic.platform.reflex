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
package dev.hiconic.servlet.decoder.impl.header;

import com.braintribe.model.generic.GenericEntity;

import dev.hiconic.servlet.decoder.api.StandardHeadersMapper;

/**
 * Base class for mappers' implementations.
 * 
 */
public abstract class AbstractStandardHeadersMapper<T extends GenericEntity> implements StandardHeadersMapper<T> {

	/**
	 * This method transforms a header name with format "-My-Header-Name" or "My-Header-Name" to "myHeaderName".
	 * 
	 * @param headerName the header name, must not be {@code null}
	 * 
	 * @return the corresponding property name, never {@code null}
	 */
	public String getPropertyNameFromStandardHeaderName(String headerName) {
		StringBuilder sb = new StringBuilder(headerName.length());
		boolean upperCase = false;
		boolean first = true;
		for (int i = 0; i < headerName.length(); i++) {
			final char character = headerName.charAt(i);
			if(character == '-') {
				// do not append, next character upper case
				upperCase = true;
			} else if (first) {
				// first character always lower case, even if it's after some dashes
				first = false;
				upperCase = false;
				sb.append(Character.toLowerCase(character));
			} else if (upperCase) {
				// after a dash, upper case
				upperCase = false;
				sb.append(Character.toUpperCase(character));
			} else {
				// lower case/sanitize the rest
				sb.append(Character.toLowerCase(character));
			}
		}
		
		return sb.toString();
	}
}
