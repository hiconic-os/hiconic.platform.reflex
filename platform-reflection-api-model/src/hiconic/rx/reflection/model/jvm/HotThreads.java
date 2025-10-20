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
package hiconic.rx.reflection.model.jvm;

import java.util.Date;
import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.reflection.model.api.PlatformReflectionResponse;


public interface HotThreads extends PlatformReflectionResponse {

	EntityType<HotThreads> T = EntityTypes.T(HotThreads.class);
	
	Date getTimestamp();
	void setTimestamp(Date timestamp);
	
	long getIntervalInMs();
	void setIntervalInMs(long intervalInMs);
	
	int getNoOfBusiestThreads();
	void setNoOfBusiestThreads(int noOfBusiestThreads);
	
	boolean getIgnoreIdleThreads();
	void setIgnoreIdleThreads(boolean ignoreIdleThreads);
	
	String getType();
	void setType(String type);

	int getThreadElementsSnapshotCount();
	void setThreadElementsSnapshotCount(int threadElementsSnapshotCount);
	
	long getThreadElementsSnapshotDelayInMs();
	void setThreadElementsSnapshotDelayInMs(long threadElementsSnapshotDelayInMs);

	List<HotThread> getHotThreadList();
	void setHotThreadList(List<HotThread> hotThreadList);
	
}
