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
package hiconic.rx.reflection.model.streampipes;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;


public interface StreamPipeBlocksInfo extends GenericEntity {
	
	EntityType<StreamPipeBlocksInfo> T = EntityTypes.T(StreamPipeBlocksInfo.class);
	
	void setNumUnused(int used);
	int getNumUnused();
	
	void setNumTotal(int total);
	int getNumTotal();
	
	void setNumMax(int allocatable);
	int getNumMax();
	
	void setMbUnused(int used);
	int getMbUnused();
	
	void setMbTotal(int total);
	int getMbTotal();
	
	void setMbAllocatable(int allocatable);
	int getMbAllocatable();
	
	void setBlockSize(int size);
	int getBlockSize();
	
	void setLocation(String location);
	String getLocation();
	
	void setInMemory(boolean isInMemory);
	boolean getInMemory();
	
	void setPoolKind(PoolKind poolKind);
	PoolKind getPoolKind();
}
