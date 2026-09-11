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
package hiconic.rx.web.server.api;

import java.util.Set;

/**
 * Describes an optional landing-page entry contributed by a web application.
 * <p>
 * Role filtering only controls discoverability. The application's services must still enforce their own authorization.
 */
public record WebAppNavigationEntry(String webAppPath, String displayName, String description, Set<String> requiredRoles, int order) {

	public WebAppNavigationEntry {
		requiredRoles = requiredRoles == null ? Set.of() : Set.copyOf(requiredRoles);
	}

	public WebAppNavigationEntry(String webAppPath, String displayName, String description, Set<String> requiredRoles) {
		this(webAppPath, displayName, description, requiredRoles, 0);
	}
}
