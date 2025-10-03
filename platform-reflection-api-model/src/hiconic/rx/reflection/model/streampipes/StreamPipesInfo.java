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

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;


public interface StreamPipesInfo extends GenericEntity {
	
	EntityType<StreamPipesInfo> T = EntityTypes.T(StreamPipesInfo.class);
	
	void setInMemoryBlocks(StreamPipeBlocksInfo info);
	StreamPipeBlocksInfo getInMemoryBlocks();
	
	void setFileBlocks(StreamPipeBlocksInfo info);
	StreamPipeBlocksInfo getFileBlocks();
	
	void setTotal(StreamPipeBlocksInfo info);
	StreamPipeBlocksInfo getTotal();
	
	void setPoolList(List<StreamPipeBlocksInfo> list);
	List<StreamPipeBlocksInfo> getPoolList();
}
