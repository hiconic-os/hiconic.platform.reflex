// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.webapi.endpoints.api.v1;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.webapi.endpoints.DdraEndpoint;

public interface ApiV1DdraEndpoint extends DdraEndpoint {
	
	EntityType<ApiV1DdraEndpoint> T = EntityTypes.T(ApiV1DdraEndpoint.class);
	
	String getProjection();
	void setProjection(String projection);

	Boolean getUseSessionEvaluation();
	void setUseSessionEvaluation(Boolean useSessionEvaluation);

	default boolean allProjection() {
		return allProjection(getProjection());
	}
	
	default boolean allProjection(String projection) {
		return projection == null || projection.equals("$all");
	}
	
}
