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
package hiconic.rx.check.model.bundle.api.response;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.check.model.result.CheckStatus;

@Abstract
public interface CbrContainer extends GenericEntity {
	EntityType<CbrContainer> T = EntityTypes.T(CbrContainer.class);

	CheckStatus getStatus();
	void setStatus(CheckStatus status);

	List<CbrAggregatable> getElements();
	void setElements(List<CbrAggregatable> elements);

}
