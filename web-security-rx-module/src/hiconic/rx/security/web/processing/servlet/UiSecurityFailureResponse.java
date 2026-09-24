// ============================================================================
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
import java.util.HashMap;
import java.util.Map;

import com.braintribe.codec.Codec;
import com.braintribe.codec.string.MapCodec;
import com.braintribe.codec.string.UrlEscapeCodec;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.security.reason.Forbidden;

import dev.hiconic.servlet.impl.util.ServletTools;
import hiconic.rx.security.web.api.WebSecurityConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UiSecurityFailureResponse implements SecurityFailureResponse {

	private final String relativeLoginPath;
	private final String accessDeniedPath;
	private final Codec<Map<String, String>, String> urlParamCodec;

	public UiSecurityFailureResponse(String relativeLoginPath, String accessDeniedPath) {
		this.relativeLoginPath = relativeLoginPath;
		this.accessDeniedPath = accessDeniedPath;
		this.urlParamCodec = urlParamCodec();
	}

	@Override
	public void respond(HttpServletRequest request, HttpServletResponse response, Reason failure) throws IOException, ServletException {
		if (failure instanceof Forbidden) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			request.getRequestDispatcher(accessDeniedPath).forward(request, response);
			return;
		}

		response.sendRedirect(loginUrl(request));
	}

	private String loginUrl(HttpServletRequest request) {
		String contextUrl = ServletTools.getServletContextUrlProxyAware(request);
		StringBuilder continueUrl = new StringBuilder(contextUrl).append(request.getServletPath());

		if (request.getPathInfo() != null)
			continueUrl.append(request.getPathInfo());
		if (request.getQueryString() != null)
			continueUrl.append('?').append(request.getQueryString());

		Map<String, String> params = new HashMap<>();
		params.put(WebSecurityConstants.REQUEST_PARAM_CONTINUE, continueUrl.toString());

		return contextUrl + relativeLoginPath + "?" + urlParamCodec.encode(params);
	}

	private Codec<Map<String, String>, String> urlParamCodec() {
		MapCodec<String, String> codec = new MapCodec<>();
		codec.setEscapeCodec(new UrlEscapeCodec());
		codec.setDelimiter("&");
		return codec;
	}
}
