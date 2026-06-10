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
package hiconic.rx.topology.api;

import java.util.Set;

import com.braintribe.model.service.api.InstanceId;

/**
 * Offers a set representing the peer application instances most likely to be up and running.
 */
public interface LiveInstances {

	/** Instance ids in the format "${applicationId}@${nodeId}" */
	Set<String> liveInstances();

	Set<String> liveInstances(InstanceId matching);

	/** Set of applicationIds. */
	Set<String> liveApplications();

}
