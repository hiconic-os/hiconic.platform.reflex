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
package hiconic.rx.access.module.processing.resource;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.braintribe.logging.Logger;
import com.braintribe.model.processing.session.api.resource.ResourceUrlBuilder;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.UrlTools;

public class RxResourceUrlBuilder implements ResourceUrlBuilder {

	private URL baseStreamingUrl;

	private Resource resource;

	private boolean download;
	private String fileName;
	private String sessionId;
	private String accessId;
	private String responseMimeType;
	private String sourceType;
	private String useCase;
	private Map<String, String> context;
	private String mimeType;
	private String md5;
	private String creator;
	private String tags;

	private static final Logger log = Logger.getLogger(RxResourceUrlBuilder.class);

	private String specification;


	public RxResourceUrlBuilder() {
		super();
	}

	@Override
	public ResourceUrlBuilder download(boolean download) {
		this.download = download;
		return this;
	}

	@Override
	public ResourceUrlBuilder fileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	@Override
	public ResourceUrlBuilder accessId(String accessId) {
		this.accessId = accessId;
		return this;
	}

	@Override
	public ResourceUrlBuilder sessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public ResourceUrlBuilder sourceType(String sourceTypeSignature) {
		this.sourceType = sourceTypeSignature;
		return this;
	}

	@Override
	public ResourceUrlBuilder useCase(String useCase) {
		this.useCase = useCase;
		return this;
	}

	@Override
	public ResourceUrlBuilder mimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	@Override
	public ResourceUrlBuilder md5(String md5) {
		this.md5 = md5;
		return this;
	}

	@Override
	public ResourceUrlBuilder creator(String creator) {
		this.creator = creator;
		return this;
	}
	
	@Override
	public ResourceUrlBuilder tags(String tags) {
		this.tags = tags;
		return this;
	}

	@Override
	public ResourceUrlBuilder specification(String specification) {
		this.specification = specification;
		return this;
	}

	@Override
	public ResourceUrlBuilder base(String baseUrl) {
		if (baseUrl != null) {
			try {
				this.baseStreamingUrl = new URL(baseUrl);
			} catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}
		return this;
	}

	/**
	 * @param download
	 *            the download to set
	 */
	public void setDownload(boolean download) {
		this.download = download;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * <p>
	 * Sets the default base URL used when no custom base URL is provided through {@link #base(String)}.
	 * 
	 * @param baseStreamingUrl
	 *            the baseStreamingUrl to set
	 */
	public void setBaseStreamingUrl(URL baseStreamingUrl) {
		this.baseStreamingUrl = baseStreamingUrl;
	}

	/**
	 * @param resource
	 *            the resource to set
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * @param sessionId
	 *            the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * <p>
	 * Sets the default access id used when none is provided through {@link #accessId(String)}.
	 * 
	 * @param accessId
	 *            the accessId to set
	 */
	public void setAccessId(String accessId) {
		this.accessId = accessId;
	}

	/**
	 * @param responseMimeType
	 *            the uploadResponseMimeType to set
	 */
	public void setResponseMimeType(String responseMimeType) {
		this.responseMimeType = responseMimeType;
	}

	@Deprecated
	@Override
	public ResourceUrlBuilder forDownload(boolean download) {
		return download(download);
	}

	@Deprecated
	@Override
	public ResourceUrlBuilder withDownloadName(String downloadName) {
		return fileName(downloadName);
	}

	@Deprecated
	@Override
	public ResourceUrlBuilder withSourceType(String sourceTypeSignature) {
		return sourceType(sourceTypeSignature);
	}

	@Deprecated
	@Override
	public ResourceUrlBuilder withFileName(String fileName) {
		return fileName(fileName);
	}

	@Deprecated
	@Override
	public ResourceUrlBuilder forAccess(String accessId) {
		return accessId(accessId);
	}

	@Deprecated
	@Override
	public ResourceUrlBuilder withSessionId(String sessionId) {
		return sessionId(sessionId);
	}

	@Override
	public String asString() {
		try {
			return asUri().toURL().toString();
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}

	public URI asUri() {

		String urlString = baseStreamingUrl + buildParameters();

		log.debug(() -> "Assembled resource streaming URL: [ " + urlString + " ]");

		try {
			return new URL(urlString).toURI();
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(urlString + " could not be parsed as a URI reference", e);
		}

	}

	private String buildParameters() {

		final Map<String, String> parameterMap = new HashMap<String, String>();
		addNotNullParameter("sessionId", sessionId, parameterMap);
		addNotNullParameter("accessId", accessId, parameterMap);
		addNotFalseParameter("download", download, parameterMap);
		addNotNullParameter("resourceId", resource != null ? resource.getId() : null, parameterMap);
		addNotNullParameter("responseMimeType", responseMimeType, parameterMap);
		addNotNullParameter("fileName", fileName, parameterMap);
		addNotNullParameter("sourceType", sourceType, parameterMap);
		addNotNullParameter("useCase", useCase, parameterMap);
		addNotNullParameter("mimeType", mimeType, parameterMap);
		addNotNullParameter("md5", md5, parameterMap);
		addNotNullParameter("creator", creator, parameterMap);
		addNotNullParameter("tags", tags, parameterMap);
		addNotNullParameter("specification", specification, parameterMap);
		
		addContext("threadName", Thread.currentThread().getName());
		addNotNullParameter("context", StringTools.encodeStringMapToString(context), parameterMap);

		try {
			return UrlTools.encodeQuery(parameterMap);
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException("Failed to build query string with parameters " + parameterMap + ": " + e.getMessage(), e);
		}

	}

	private void addNotNullParameter(String key, String value, Map<String, String> parameterMap) {
		if (value == null || value.trim().isEmpty())
			return;
		parameterMap.put(key, value);
	}

	private void addNotFalseParameter(String key, boolean value, Map<String, String> parameterMap) {
		if (!value)
			return;
		parameterMap.put(key, Boolean.toString(value));
	}

	public void addContext(String key, String value) {
		if (key == null) {
			throw new IllegalArgumentException("The key must not be null.");
		}
		if (context == null) {
			context = new HashMap<>();
		}
		if (value == null) {
			context.remove(key);
		} else {
			context.put(key, value);
		}
	}

	public Map<String, String> getContext() {
		return context;
	}

	public void setContext(Map<String, String> context) {
		this.context = context;
	}

}
