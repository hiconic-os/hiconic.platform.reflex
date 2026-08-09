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
package hiconic.rx.platform.module.wire;

import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.initializer.api.InitializerBackendContract;
import hiconic.rx.initializer.api.InitializerContract;
import hiconic.rx.platform.module.wire.space.CoreRxPlatformModuleSpace;
import hiconic.rx.push.api.PushContract;

public enum CoreRxPlatformModule implements RxModule<CoreRxPlatformModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(PushContract.class, CoreRxPlatformModuleSpace.class);
		exports.bind(InitializerContract.class, CoreRxPlatformModuleSpace.class);
		exports.bind(InitializerBackendContract.class, CoreRxPlatformModuleSpace.class);
	}

}
