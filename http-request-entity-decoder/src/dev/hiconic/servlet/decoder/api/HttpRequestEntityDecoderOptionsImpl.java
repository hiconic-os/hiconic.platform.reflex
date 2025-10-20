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
package dev.hiconic.servlet.decoder.api;

import java.util.ArrayList;
import java.util.List;

/*package*/ class HttpRequestEntityDecoderOptionsImpl implements HttpRequestEntityDecoderOptions {
	
	private boolean ignoringHeaders;
	private boolean ignoringUnmappedUrlParameters;
	private boolean ignoringUnmappedHeaders;
	private boolean nullAware;
	private PropertyTypeResolver propertyTypeResolver;
	private List<String> ignoredParameters = new ArrayList<>();

	@Override
	public boolean isIgnoringHeaders() {
		return ignoringHeaders;
	}

	@Override
	public HttpRequestEntityDecoderOptions setIgnoringHeaders(boolean ignoringHeaders) {
		this.ignoringHeaders = ignoringHeaders;
		return this;
	}

	@Override
	public boolean isIgnoringUnmappedUrlParameters() {
		return ignoringUnmappedUrlParameters;
	}

	@Override
	public HttpRequestEntityDecoderOptions setIgnoringUnmappedUrlParameters(boolean ignoringUnmappedUrlParameters) {
		this.ignoringUnmappedUrlParameters = ignoringUnmappedUrlParameters;
		return this;
	}

	@Override
	public boolean isIgnoringUnmappedHeaders() {
		return ignoringUnmappedHeaders;
	}

	@Override
	public HttpRequestEntityDecoderOptions setIgnoringUnmappedHeaders(boolean ignoringUnmappedHeaders) {
		this.ignoringUnmappedHeaders = ignoringUnmappedHeaders;
		return this;
	}

	@Override
	public PropertyTypeResolver getPropertyTypeResolver() {
		return propertyTypeResolver;
	}

	@Override
	public HttpRequestEntityDecoderOptions setPropertyTypeResolver(PropertyTypeResolver propertyTypeResolver) {
		this.propertyTypeResolver = propertyTypeResolver;
		return this;
	}

	@Override
	public List<String> getIgnoredParameters() {
		return ignoredParameters;
	}

	@Override
	public HttpRequestEntityDecoderOptions setIgnoredParameters(List<String> ignoredParameters) {
		this.ignoredParameters = ignoredParameters;
		return this;
	}

	@Override
	public HttpRequestEntityDecoderOptions addIgnoredParameter(String ignoredParameter) {
		if (ignoredParameters == null) {
			ignoredParameters = new ArrayList<>();
		}
		
		ignoredParameters.add(ignoredParameter);
		return this;
	}

	@Override
	public HttpRequestEntityDecoderOptions setNullAware(boolean nullAware) {
		this.nullAware = nullAware;
		return this;
	}

	@Override
	public boolean isNullAware() {
		return nullAware;
	}

}
