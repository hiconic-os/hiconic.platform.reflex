// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.module.api.resource;

import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.api.ResourceHandle;

import com.braintribe.model.resource.source.PackagedSource;

/** Builds a {@link Resource} view of an indexed packaged resource. */
public interface RxPackagedResourceBuilder {

	String path();

	ResourceHandle asHandle();

	RxPackagedResourceBuilder withMimeType();

	RxPackagedResourceBuilder withFileSize();

	RxPackagedResourceBuilder withMd5();

	RxPackagedResourceBuilder withSpecification();

	default RxPackagedResourceBuilder withHttpMetadata() {
		return withMimeType().withFileSize().withMd5();
	}

	/**
	 * Returns a new Resource backed by the {@link #asSource() packaged source}, thus it is at once persistable and readable: the address is fully
	 * modeled, and the source carries a reader for it.
	 * <p>
	 * Requested metadata is computed lazily and cached by the resolver, but copied onto each new Resource, so callers cannot mutate shared state.
	 * Every {@link Resource#openStream()} call opens a fresh stream; payload bytes are never cached.
	 */
	Resource asResource();

	PackagedSource asSource();
}
