// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.log.reflection.model.configuration;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogReflectionConfiguration extends GenericEntity {
	EntityType<LogReflectionConfiguration> T = EntityTypes.T(LogReflectionConfiguration.class);

	@Initializer("{'log-reflection-reader','log-reflection-manager'}")
	Set<String> getReaderRoles();
	void setReaderRoles(Set<String> readerRoles);

	@Initializer("{'log-reflection-manager'}")
	Set<String> getManagerRoles();
	void setManagerRoles(Set<String> managerRoles);

	/**
	 * Cadence of non-blocking live log queries. A new query is only started after the previous one completed, so slow
	 * responses are coalesced instead of accumulating.
	 */
	@Initializer("1000")
	int getTailPollIntervalMillis();
	void setTailPollIntervalMillis(int tailPollIntervalMillis);

	/**
	 * Maximum number of structured records retained per node while at least one live-log lease is active. The live
	 * collector is detached when its lease expires and therefore has no permanent logging or heap cost.
	 */
	@Initializer("10000")
	int getLiveLogCapacity();
	void setLiveLogCapacity(int liveLogCapacity);

	/**
	 * Grace period after the latest structured-live query before the node detaches its collector and releases its
	 * buffer. Polling renews the lease; a dropped browser therefore needs no explicit unsubscribe operation.
	 */
	@Initializer("30000L")
	long getLiveLogLeaseMillis();
	void setLiveLogLeaseMillis(long liveLogLeaseMillis);
}
