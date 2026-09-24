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

import com.braintribe.model.processing.resource.packaged.api.PackagedResourceResolver;
import com.braintribe.model.resource.source.PackagedSource;

/**
 * Resolves resources attached to the application via GM's indexed resources mechanism.
 */
public interface RxPackagedResourceResolver extends PackagedResourceResolver {

	/** Resolves an indexed resource by its path. */
	RxPackagedResourceBuilder resource(String relativePath);

	/** Resolves an indexed resources by its path and origin artifact, */
	RxPackagedResourceBuilder resource(String artifact, String relativePath);

	/**
	 * A resolver for a folder below this one, so that a module which owns such a folder can address its files by a short path.
	 * <p>
	 * The platform attaches no meaning to a folder. A module that puts files there gives them their meaning, for example by serving them.
	 */
	RxPackagedResourceResolver below(String folder);

	/** Convenient method for resolving an indexed resource denoted by a {@link PackagedSource}. */
	default RxPackagedResourceBuilder resource(PackagedSource source) {
		return source.getArtifact() == null || source.getArtifact().isBlank() //
				? resource(source.getPath()) //
				: resource(source.getArtifact(), source.getPath());
	}

	/** Returns the inventory of all indexed resources */
	RxPackagedResourceInventory inventory();

}
