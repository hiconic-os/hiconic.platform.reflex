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
package hiconic.rx.reflection.model.system.memory;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;


public interface Memory extends GenericEntity {

	EntityType<Memory> T = EntityTypes.T(Memory.class);
	
	long getTotal();
	void setTotal(long total);

	Double getTotalInGb();
	void setTotalInGb(Double totalInGb);

	long getAvailable();
	void setAvailable(long available);

	Double getAvailableInGb();
	void setAvailableInGb(Double availableInGb);

	long getSwapTotal();
	void setSwapTotal(long swapTotal);

	Double getSwapTotalInGb();
	void setSwapTotalInGb(Double swapTotalInGb);

	long getSwapUsed();
	void setSwapUsed(long swapUsed);
	
	Double getSwapUsedInGb();
	void setSwapUsedInGb(Double swapUsedInGb);

	List<String> getMemoryBanksInformation();
	void setMemoryBanksInformation(List<String> memoryBanksInformation);
}
