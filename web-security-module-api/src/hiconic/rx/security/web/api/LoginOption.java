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
package hiconic.rx.security.web.api;

/**
 * A login alternative contributed by an authentication module and presented by the standard web-security login page.
 * <p>
 * Web security deliberately treats the target as an opaque application-relative path. It only propagates the optional
 * {@code continue} request parameter when it builds the link.
 */
public final class LoginOption {

	private final String id;
	private final String label;
	private final String targetPath;
	private final String iconPath;

	public LoginOption(String id, String label, String targetPath, String iconPath) {
		this.id = requireText(id, "id");
		this.label = requireText(label, "label");
		this.targetPath = requireText(targetPath, "targetPath");
		this.iconPath = iconPath;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public String getIconPath() {
		return iconPath;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException("Login option " + name + " must not be blank");

		return value;
	}
}
