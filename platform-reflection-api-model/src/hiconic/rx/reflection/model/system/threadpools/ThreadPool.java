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
package hiconic.rx.reflection.model.system.threadpools;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ThreadPool extends GenericEntity {

	EntityType<ThreadPool> T = EntityTypes.T(ThreadPool.class);

	String getName();
	void setName(String name);

	Integer getActiveThreads();
	void setActiveThreads(Integer activeThreads);

	Long getTotalExecutions();
	void setTotalExecutions(Long totalExecutions);

	Long getAverageRunningTimeMs();
	void setAverageRunningTimeMs(Long averageRunningTimeMs);

	Integer getCorePoolSize();
	void setCorePoolSize(Integer corePoolSize);

	Integer getMaxPoolSize();
	void setMaxPoolSize(Integer maxPoolSize);

	Integer getPoolSize();
	void setPoolSize(Integer poolSize);

	Integer getPendingTasksInQueue();
	void setPendingTasksInQueue(Integer pendingTasksInQueue);

	Double getAveragePendingTimeInMs();
	void setAveragePendingTimeInMs(Double averagePendingTimeInMs);

	Long getTimeSinceLastExecutionMs();
	void setTimeSinceLastExecutionMs(Long timeSinceLastExecutionMs);

	Long getMaximumEnqueuedTimeInMs();
	void setMaximumEnqueuedTimeInMs(Long maximumEnqueuedTimeInMs);

	Long getMinimumEnqueuedTimeInMs();
	void setMinimumEnqueuedTimeInMs(Long minimumEnqueuedTimeInMs);

}
