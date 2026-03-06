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

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/* IMPLEMENTATION NOTE: This is not a predicate, because we need to distinguish whether the MD was set or not, because the default 
 * boolean value is different for different HTTP methods. See for usages of SingleDdraMapping#getAnnounceAsMultipart(). */
public interface ResponseAsMultipart extends EntityTypeMetaData {

	EntityType<ResponseAsMultipart> T = EntityTypes.T(ResponseAsMultipart.class);

	Boolean getAnnounceAsMultipart();
	void setAnnounceAsMultipart(Boolean announceAsMultipart);

	static ResponseAsMultipart create(Boolean announceAsMultipart) {
		ResponseAsMultipart result = T.create();
		result.setAnnounceAsMultipart(announceAsMultipart);
		return result;
	}
}
