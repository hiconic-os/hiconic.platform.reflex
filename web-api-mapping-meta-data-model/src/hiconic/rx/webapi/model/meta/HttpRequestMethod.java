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
package hiconic.rx.webapi.model.meta;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

// TODO rename HttpRequestMethod
public enum HttpRequestMethod implements EnumBase<HttpRequestMethod> {
	GET,
	POST,
	PUT,
	PATCH,
	DELETE;

	public static final EnumType<HttpRequestMethod> T = EnumTypes.T(HttpRequestMethod.class);

	@Override
	public EnumType<HttpRequestMethod> type() {
		return T;
	}
}
