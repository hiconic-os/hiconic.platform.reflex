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
package hiconic.rx.access.model.md;

import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.ModelMetaData;

public interface InterceptAccessWith extends ModelMetaData {
	EntityType<InterceptAccessWith> T = EntityTypes.T(InterceptAccessWith.class);

	@Transient
	<A> A getAssociate();
	void setAssociate(Object associate);

	static InterceptAccessWith create(Object accessAspect) {
		InterceptAccessWith interceptAccessWith = InterceptAccessWith.T.create();
		interceptAccessWith.setAssociate(accessAspect);
		return interceptAccessWith;
	}
}
