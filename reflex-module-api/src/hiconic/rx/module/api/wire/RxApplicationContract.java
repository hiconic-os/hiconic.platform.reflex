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
package hiconic.rx.module.api.wire;

import com.braintribe.model.service.api.InstanceId;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.state.RxApplicationStateManager;

public interface RxApplicationContract extends WireSpace {

	RxLogManager logManager();

	RxApplicationStateManager stateManager();

	/** The name of the application which the platform is hosting given by the applicationName property in META-INF/rx-app.properties */
	String applicationName();

	/** The technical application id. */
	String applicationId();

	/** The nodeId of this instance in distributed systems */
	String nodeId();

	/** Holds the applicationId and nodeId information (not sure why this was introduced). */
	InstanceId instanceId();

}
