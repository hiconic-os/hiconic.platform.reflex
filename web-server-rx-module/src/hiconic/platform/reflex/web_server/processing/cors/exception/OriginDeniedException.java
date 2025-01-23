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
package hiconic.platform.reflex.web_server.processing.cors.exception;

import jakarta.servlet.http.HttpServletResponse;

public class OriginDeniedException extends CorsException {

	private static final long serialVersionUID = 1L;

	public OriginDeniedException() {
		super();
	}

	public OriginDeniedException(String message, Throwable cause) {
		super(message, cause);
	}

	public OriginDeniedException(String message) {
		super(message);
	}

	public OriginDeniedException(Throwable cause) {
		super(cause);
	}

	@Override
	public int getHttpResponseCode() {
		return HttpServletResponse.SC_FORBIDDEN;
	}
	
}
