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
package hiconic.rx.initializer.api;

import hiconic.rx.module.api.wire.RxExportContract;

/** Extension point for replacing the platform's default file-system initializer backend before application readiness. */
public interface InitializerBackendContract extends RxExportContract {

	void setBackend(InitializerBackend backend);
}
