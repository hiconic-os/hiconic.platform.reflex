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
package hiconic.rx.webapi.client.api;

import java.util.Set;

public interface HttpConstants {
	
	final int HTTP_CODE_OK = 200;
	final int HTTP_CODE_CREATED = 201;
	final int HTTP_CODE_ACCEPTED = 202;

	final String HTTP_METHOD_GET = "GET"; 
	final String HTTP_METHOD_POST = "POST";
	final String HTTP_METHOD_PUT = "PUT";
	final String HTTP_METHOD_PATCH = "PATCH";
	final String HTTP_METHOD_DELETE = "DELETE";
	final String HTTP_METHOD_HEAD = "HEAD";
	final String HTTP_METHOD_OPTIONS = "OPTIONS"; 
	final String HTTP_METHOD_TRACE = "TRACE";

	final String HTTP_HEADER_ACCEPT = "ACCEPT";
	final String HTTP_HEADER_CONTENTTYPE = "CONTENT-TYPE";

	final Set<Integer> DEFAULT_SUCCESS_CODES = Set.of(HTTP_CODE_OK, HTTP_CODE_CREATED, HTTP_CODE_ACCEPTED);
	final String DEFAULT_PATH = "/";
	final String DEFAULT_MIME_TYPE = "application/json";
	
}
