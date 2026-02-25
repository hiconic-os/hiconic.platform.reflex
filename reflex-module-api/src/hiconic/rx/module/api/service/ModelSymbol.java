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
package hiconic.rx.module.api.service;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.common.symbol.Symbol;

@FunctionalInterface
public interface ModelSymbol extends Symbol {

	String name();

	static ModelSymbol configured(ArtifactReflection artifactReflection) {
		return prefixed("configured", artifactReflection);
	}
	
	static ModelSymbol prefixed(String prefix, ArtifactReflection artifactReflection) {
		return of(artifactReflection.groupId() + ":" + prefix + "-" + artifactReflection.artifactId());
	}

	static ModelSymbol of(String name) {
		return () -> name;
	}

}
