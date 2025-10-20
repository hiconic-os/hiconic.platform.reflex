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
package hiconic.rx.webapi.endpoints.v2;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.query.OrderingDirection;

public interface DdraGetEntitiesEndpoint extends DdraGetEntitiesEndpointBase {

	EntityType<DdraGetEntitiesEndpoint> T = EntityTypes.T(DdraGetEntitiesEndpoint.class);

	Integer getStartIndex();
	void setStartIndex(Integer startIndex);
	
	Integer getMaxResults();
	void setMaxResults(Integer maxResults);
	
	List<String> getOrderBy();
	void setOrderBy(List<String> orderBy);
	
	List<OrderingDirection> getOrderDirection();
	void setOrderDirection(List<OrderingDirection> orderDirection);
	
	boolean getDistinct();
	void setDistinct(boolean distinct);
}
