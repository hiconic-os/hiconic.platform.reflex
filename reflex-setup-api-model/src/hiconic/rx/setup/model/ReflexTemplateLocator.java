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
package hiconic.rx.setup.model;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.platform.reflex._ReflexSetupApiModel_;

public class ReflexTemplateLocator {

	/** Returns a fully qualified artifact name for an artifact in this same group. */
	public static String template(String artifactId) {
		ArtifactReflection ar = _ReflexSetupApiModel_.reflection;
		return ar.groupId() + ":" + artifactId + "#" + ReflexTemplateLocator.removeRevision(ar.version());
	}

	private static String removeRevision(String version) {
		int f = version.indexOf('.');
		int l = version.lastIndexOf('.');
		if (f < 0 || l < 0 || f == l)
			throw new IllegalArgumentException("Unexpected version format. Version: " + version);
		return version.substring(0, l);
	}

}
