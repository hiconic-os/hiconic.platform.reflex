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

import hiconic.rx.resource.model.packaged.PackagedResourceSource;

public interface RxPackagedResourceResolver {

	RxPackagedResourceBuilder resource(String relativePath);

	/** Resolves an indexed path within a specific artifact. */
	default RxPackagedResourceBuilder resource(String artifact, String artifactRelativePath) {
		throw new IllegalArgumentException("Artifact-scoped packaged resources are not supported by this resolver: " + artifact);
	}

	default RxPackagedResourceBuilder resource(PackagedResourceSource source) {
		return source.getArtifact() == null || source.getArtifact().isBlank()
				? resource(source.getPath())
				: resource(source.getArtifact(), source.getPath());
	}

	RxPackagedResourceInventory inventory();
}
