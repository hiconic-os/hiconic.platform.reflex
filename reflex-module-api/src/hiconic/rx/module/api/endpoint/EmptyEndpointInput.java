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
package hiconic.rx.module.api.endpoint;

import java.util.Collections;
import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class EmptyEndpointInput implements EndpointInput {
	public final static EndpointInput INSTANCE = new EmptyEndpointInput();
	@Override
	public <I extends GenericEntity> I findInput(EntityType<I> inputType) {
		return null;
	}
	
	@Override
	public <I extends GenericEntity> List<I> findInputs(EntityType<I> inputType) {
		return Collections.emptyList();
	}
}
