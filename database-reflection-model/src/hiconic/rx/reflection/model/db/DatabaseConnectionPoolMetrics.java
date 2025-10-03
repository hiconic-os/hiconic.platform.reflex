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
package hiconic.rx.reflection.model.db;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface DatabaseConnectionPoolMetrics extends GenericEntity {

	EntityType<DatabaseConnectionPoolMetrics> T = EntityTypes.T(DatabaseConnectionPoolMetrics.class);

	Integer getActiveConnections();
	void setActiveConnections(Integer activeConnections);

	Integer getIdleConnections();
	void setIdleConnections(Integer idleConnections);

	Integer getThreadsAwaitingConnections();
	void setThreadsAwaitingConnections(Integer threadsAwaitingConnections);

	Integer getTotalConnections();
	void setTotalConnections(Integer totalConnections);

	Double getWaitTimeOneMinuteRate();
	void setWaitTimeOneMinuteRate(Double waitTimeOneMinuteRate);

	Double getWaitTimeFiveMinutesRate();
	void setWaitTimeFiveMinutesRate(Double waitTimeFiveMinutesRate);

	Double getWaitTimeFifteenMinutesRate();
	void setWaitTimeFifteenMinutesRate(Double waitTimeFifteenMinutesRate);

	Double getWaitTimeMeanRate();
	void setWaitTimeMeanRate(Double waitTimeMeanRate);

	Long getLeaseCount();
	void setLeaseCount(Long leaseCount);

	Long getUsageMinTime();
	void setUsageMinTime(Long usageMinTime);

	Long getUsageMaxTime();
	void setUsageMaxTime(Long usageMaxTime);

	Double getUsageMedianTime();
	void setUsageMedianTime(Double usageMedianTime);

	Double getUsageMeanTime();
	void setUsageMeanTime(Double usageMeanTime);

}
