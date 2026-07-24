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
package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;
import java.io.InputStream;

import com.braintribe.model.resource.Resource;

import hiconic.rx.module.api.wire.RxPackagedPublicResourcesContract;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Streams indexed packaged public resources without exposing their classpath or packaging backing. */
public class PackagedPublicResourceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private RxPackagedPublicResourcesContract resources;
	private String resourcePathPrefix = "";

	public void setResources(RxPackagedPublicResourcesContract resources) {
		this.resources = resources;
	}

	public void setResourcePathPrefix(String resourcePathPrefix) {
		if (resourcePathPrefix == null || resourcePathPrefix.isBlank()) {
			this.resourcePathPrefix = "";
			return;
		}

		String normalized = resourcePathPrefix.startsWith("/") ? resourcePathPrefix : "/" + resourcePathPrefix;
		this.resourcePathPrefix = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		serve(request, response, true);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
		serve(request, response, false);
	}

	private void serve(HttpServletRequest request, HttpServletResponse response, boolean includeBody) throws IOException {
		String path = request.getPathInfo();
		if (path == null || path.isBlank() || path.equals("/") || path.endsWith("/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String resourcePath = resourcePathPrefix + path;
		if (!resources.inventory().contains(resourcePath)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Resource resource = resources.resource(resourcePath).withHttpMetadata().asResource();
		applyHeaders(response, resource);

		String etag = etag(resource);
		if (etagMatches(request.getHeader("If-None-Match"), etag)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		if (!includeBody)
			return;

		try (InputStream in = resource.openStream()) {
			in.transferTo(response.getOutputStream());
		}
	}

	private static void applyHeaders(HttpServletResponse response, Resource resource) {
		if (resource.getMimeType() != null)
			response.setContentType(resource.getMimeType());
		if (resource.getFileSize() != null)
			response.setContentLengthLong(resource.getFileSize());
		String etag = etag(resource);
		if (etag != null)
			response.setHeader("ETag", etag);
	}

	private static String etag(Resource resource) {
		return resource.getMd5() == null ? null : '"' + resource.getMd5() + '"';
	}

	private static boolean etagMatches(String ifNoneMatch, String etag) {
		if (ifNoneMatch == null || etag == null)
			return false;
		for (String candidate : ifNoneMatch.split(",")) {
			String normalized = candidate.trim();
			if (normalized.equals("*"))
				return true;
			if (normalized.startsWith("W/"))
				normalized = normalized.substring(2).trim();
			if (normalized.equals(etag))
				return true;
		}
		return false;
	}
}
