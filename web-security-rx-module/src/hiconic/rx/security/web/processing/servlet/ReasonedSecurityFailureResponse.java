+// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.security.web.processing.servlet;

import java.io.IOException;

import com.braintribe.exception.HttpException;
import com.braintribe.exception.LogPreferences;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.logging.Logger.LogLevel;
import com.braintribe.model.service.api.result.Unsatisfied;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ReasonedSecurityFailureResponse implements SecurityFailureResponse {

	@Override
	public void respond(HttpServletRequest request, HttpServletResponse response, Reason failure) throws IOException, ServletException {
		HttpException exception = new HttpException(statusCode(failure), failure.stringify());
		exception.setLogPreferences(new LogPreferences(LogLevel.INFO, false, LogLevel.TRACE));
		exception.withPayload(Unsatisfied.from(Maybe.empty(failure)));
		throw exception;
	}

	private int statusCode(Reason failure) {
		return failure instanceof Forbidden ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED;
	}
}
