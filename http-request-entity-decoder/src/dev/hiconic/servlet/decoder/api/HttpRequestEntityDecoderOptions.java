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

import java.util.List;

/**
 * The options object when decoding Http Requests.
 */
public interface HttpRequestEntityDecoderOptions {
	
	/**
	 * @return a fresh set of default options, to be customized by the called
	 */
	static HttpRequestEntityDecoderOptions defaults() {
		return new HttpRequestEntityDecoderOptionsImpl();
	}

	/**
	 * @return {@code true} if the decoder should not read the header parameters (both standard and generic), defaults
	 *         to {@code false}
	 */
	boolean isIgnoringHeaders();

	/**
	 * Set ignoringHeaders.
	 * 
	 * @param ignoringHeaders
	 *            {@code true} if the decoder should not read the header parameters (both standard and generic),
	 *            defaults to {@code false}
	 * @return this
	 */
	HttpRequestEntityDecoderOptions setIgnoringHeaders(boolean ignoringHeaders);

	/**
	 * @return {@code true} if the decoder should not throw a bad request when no entity is found for a URL parameter,
	 *         defaults to {@code false}
	 */
	boolean isIgnoringUnmappedUrlParameters();

	/**
	 * Set ignoringUnmappedUrlParameters.
	 * 
	 * @param ignoringUnmappedUrlParameters
	 *            {@code true} if the decoder should not throw a bad request when no entity is found for a URL
	 *            parameter, defaults to {@code false}
	 * @return this
	 */
	HttpRequestEntityDecoderOptions setIgnoringUnmappedUrlParameters(boolean ignoringUnmappedUrlParameters);

	/**
	 * @return {@code true} if the decoder should not throw a bad request when no entity is found for a header (both
	 *         standard and generic), defaults to {@code false}
	 */
	boolean isIgnoringUnmappedHeaders();

	/**
	 * Sets ignoringUnmappedHeaders.
	 * 
	 * @param ignoringUnmappedHeaders
	 *            {@code true} if the decoder should not throw a bad request when no entity is found for a header (both
	 *            standard and generic), defaults to {@code false}
	 * @return this
	 */
	HttpRequestEntityDecoderOptions setIgnoringUnmappedHeaders(boolean ignoringUnmappedHeaders);

	/**
	 * @return the type resolver that the decoder use for object properties, or {@code null} if none
	 */
	PropertyTypeResolver getPropertyTypeResolver();

	/**
	 * Set the resolver that the decoder use for object properties.
	 * 
	 * @param propertyTypeResolver the type resolver, or {@code null} if none
	 * 
	 * @return this
	 */
	HttpRequestEntityDecoderOptions setPropertyTypeResolver(PropertyTypeResolver propertyTypeResolver);
	
	List<String> getIgnoredParameters();
	HttpRequestEntityDecoderOptions setIgnoredParameters(List<String> ignoredParameters);
	HttpRequestEntityDecoderOptions addIgnoredParameter(String ignoredParameter);

	HttpRequestEntityDecoderOptions setNullAware(boolean nullAware);
	boolean isNullAware();
}
