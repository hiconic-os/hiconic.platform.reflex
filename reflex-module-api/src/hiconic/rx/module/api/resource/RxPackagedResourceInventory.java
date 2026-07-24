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

import java.util.List;
import java.util.Set;

/** Immutable inventory assembled from all {@code META-INF/classpath-index.txt} files. */
public interface RxPackagedResourceInventory {

	Set<String> resourcePaths();

	boolean contains(String relativePath);

	/** Lists direct resources and directories below the supplied logical directory. Empty string denotes the root. */
	List<RxPackagedResourceEntry> list(String relativeDirectory);
}
