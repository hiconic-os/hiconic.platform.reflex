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

import com.braintribe.model.resource.source.PackagedSource;

public interface RxPackagedResourceResolver {

	RxPackagedResourceBuilder resource(String relativePath);

	/** Resolves an indexed path within a specific artifact. */
	RxPackagedResourceBuilder resource(String artifact, String artifactRelativePath);

	/**
	 * A resolver for a folder below this one, so that a module which owns such a folder can address its files by a short path.
	 * <p>
	 * The platform attaches no meaning to a folder. A module that puts files there gives them their meaning, for example by serving them.
	 */
	RxPackagedResourceResolver below(String folder);

	default RxPackagedResourceBuilder resource(PackagedSource source) {
		return source.getArtifact() == null || source.getArtifact().isBlank()
				? resource(source.getPath())
				: resource(source.getArtifact(), source.getPath());
	}

	RxPackagedResourceInventory inventory();
}
