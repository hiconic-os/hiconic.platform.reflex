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
package hiconic.rx.access.module.api;

import java.util.Set;

public interface AccessDomains {
	
	static String accessDataModelName(AccessSymbol accessId) {
		return accessDataModelName(accessId.name());
	}

	static String accessDataModelName(String accessId) {
		return "rx:configured-" + accessId + "-data-model";
	}

	Set<String> domainIds();

	boolean hasDomain(String domainId);

	/** Returns the {@link AccessDomain} for given domainId or <tt>null</tt> if no such domain exists. */
	AccessDomain byId(String domainId);
}
