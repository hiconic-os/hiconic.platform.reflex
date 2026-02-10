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
 * This metadata controls the sectioning of requests in named groups for a more structured api reflection
 * <p>
 * Annotation: {@link hiconic.rx.webapi.model.annotation.RequestSection}
 * 
 * @author dirk.scheffler
 */
public interface RequestSection extends EntityTypeMetaData {

	EntityType<RequestSection> T = EntityTypes.T(RequestSection.class);

	/**
	 * The name of the section in which associated requests are listed
	 */
	@Mandatory
	String getName();
	void setName(String name);
}
