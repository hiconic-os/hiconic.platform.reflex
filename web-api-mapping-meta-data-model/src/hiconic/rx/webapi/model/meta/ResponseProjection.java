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

/** Specifies, via a property path, which part of the annotated request's response will be returned. */
public interface ResponseProjection extends EntityTypeMetaData {

	EntityType<ResponseProjection> T = EntityTypes.T(ResponseProjection.class);

	/**
	 * Path describing what to return, specified as a dot separated sequence of property names. I.e. the first one is a property of the response type.
	 * E.g.{@code "company.logo"}
	 */
	@Mandatory
	String getPath();
	void setPath(String path);

	static ResponseProjection create(String path) {
		ResponseProjection result = T.create();
		result.setPath(path);
		return result;
	}
}
