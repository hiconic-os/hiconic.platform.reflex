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
package hiconic.rx.topology.module.wire;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.topology.api.TopologyContract;
import hiconic.rx.topology.module.wire.space.TopologyViaMessagingModuleSpace;

public enum TopologyViaMessagingRxModule implements RxModule<TopologyViaMessagingModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(TopologyContract.class, TopologyViaMessagingModuleSpace.class);
	}
	
}