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

import static dev.hiconic.servlet.decoder.impl.header.HeaderValueType.DATE;
import static dev.hiconic.servlet.decoder.impl.header.HeaderValueType.INT;
import static dev.hiconic.servlet.decoder.impl.header.HeaderValueType.STRING;
import static dev.hiconic.servlet.decoder.impl.header.HeaderValueType.STRING_LIST;

import java.util.Map;

import com.braintribe.utils.MapTools;

/**
 * This class contains the map of well known headers' name with their types: {@link #WELL_KNOWN_HEADERS}.
 * 
 */
public class HeaderValueTypes {
	
	/**
	 * Contains all well known headers' types, by name.
	 */
	//@formatter:off
	public static final Map<String, HeaderValueType> WELL_KNOWN_HEADERS = MapTools.getParameterizedMap(
			String.class, 			HeaderValueType.class,
			"accept", 				STRING_LIST,
			"acceptCharset", 		STRING_LIST,
			"acceptEncoding", 		STRING_LIST,
			"acceptLanguage", 		STRING_LIST,
			"acceptRange", 			STRING_LIST,
			"age", 					INT,
			"allow", 				STRING_LIST,
			"authorization", 		STRING,
			"cacheControl", 			STRING_LIST,
			"connection", 			STRING_LIST,
			"contentEncoding", 		STRING_LIST,
			"contentLanguage", 		STRING_LIST,
			"contentLength", 		INT,
			"contentLocation", 		STRING,
			"contentMd5", 			STRING,
			"contentRange", 			STRING,
			"contentType", 			STRING,
			"date", 					DATE,
			"eTag", 					STRING,
			"expect", 				STRING_LIST,
			"expires", 				DATE, 
			"from", 					STRING,
			"host", 					STRING,
			"ifMatch", 				STRING_LIST, // TODO list or "*"
			"ifModifiedSince",	 	DATE,
			"ifNoneMatch", 			STRING_LIST, // TODO list or "*"
			"ifRange", 				STRING, // TODO date or string (entity-tag)
			"ifUnmodifiedSince",		DATE,
			"lastModified", 			DATE,
			"location", 				STRING,
			"maxForwards", 			INT,
			"pragma", 				STRING_LIST,
			"proxyAuthenticate", 	STRING_LIST,
			"proxyAuthorization",	STRING,
			"range", 				STRING,
			"referer", 				STRING,
			"retryAfter", 			STRING, // TODO date or int
			"server", 				STRING,
			"te", 					STRING_LIST,
			"trailer", 				STRING_LIST,
			"transferEncoding", 		STRING_LIST,
			"upgrade", 				STRING_LIST,
			"userAgent", 			STRING_LIST,
			"vary", 					STRING_LIST, // TODO list or "*"
			"via", 					STRING_LIST,
			"warning", 				STRING_LIST,
			"wwwAuthenticate", 		STRING_LIST
			);
	//@formatter:on
}
