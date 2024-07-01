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
// ============================================================================
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Maps a ServicePreProcessor from the service-api using the transient 'associate' property. It can be applied to ServiceRequest entity types.")
public interface PreProcessWith extends InterceptWith {
	EntityType<PreProcessWith> T = EntityTypes.T(PreProcessWith.class);

	@Override
	default InterceptionType interceptionType() {
		return InterceptionType.preProcess;
	}
}
