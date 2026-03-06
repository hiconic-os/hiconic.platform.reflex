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

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/**
 * Specifies how many levels of nested entities are included in the response.
 * <p>
 * The default is 3 (backwards compatibility), but the only reasonable default would be "reachable", meaning the web-api endpoint returns the result
 * as it was returned by the service processor (which knows what the correct result is).
 */
public interface ResponseDepth extends EntityTypeMetaData {

	EntityType<ResponseDepth> T = EntityTypes.T(ResponseDepth.class);

	/**
	 * Value is one of:
	 * <ul>
	 * <li>a positive integer (e.g. 3)
	 * <li>"shallow"
	 * <li>"reachable"
	 * </ul>
	 */
	@Mandatory
	String getDepth();
	void setDepth(String depth);

	static ResponseDepth create(String depth) {
		ResponseDepth result = T.create();
		result.setDepth(depth);
		return result;
	}

}
