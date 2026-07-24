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
package hiconic.rx.access.smood.module.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.access.smood.model.configuration.SmoodAccess;
import hiconic.rx.access.smood.processing.SmoodAccessExpert;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class SmoodAccessRxModuleSpace implements RxModuleContract {

	@Import
	private AccessExpertContract accessExperts;

	@Override
	public void onDeploy() {
		accessExperts.registerAccessExpert(SmoodAccess.T, smoodAccessExpert());
	}

	@Managed
	private SmoodAccessExpert smoodAccessExpert() {
		return new SmoodAccessExpert();
	}

}
