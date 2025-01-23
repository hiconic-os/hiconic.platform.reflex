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
package hiconic.platform.reflex.web_server.processing.cors.handler;

public enum CorsHeaders {
	
	accessControlAllowCredentials("Access-Control-Allow-Credentials"),
	accessControlAllowHeaders	 ("Access-Control-Allow-Headers"),
	accessControlAllowMethods	 ("Access-Control-Allow-Methods"),
	accessControlAllowOrigin	 ("Access-Control-Allow-Origin"),
	accessControlExposeHeaders	 ("Access-Control-Expose-Headers"),
	accessControlMaxAge			 ("Access-Control-Max-Age"),
	accessControlRequestHeaders	 ("Access-Control-Request-Headers"),
	accessControlRequestMethod	 ("Access-Control-Request-Method"),
	host						 ("Host"),
	origin						 ("Origin"),
	vary						 ("Vary");
	
	private String headerName;
	
	private CorsHeaders(String headerName) {
		this.headerName = headerName;
	}
	
	public String getHeaderName() {
		return headerName;
	}
	
}
