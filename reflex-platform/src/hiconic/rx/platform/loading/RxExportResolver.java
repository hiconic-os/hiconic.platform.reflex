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
package hiconic.rx.platform.loading;

import java.util.Map;

import com.braintribe.wire.api.space.ContractResolution;
import com.braintribe.wire.api.space.ContractSpaceResolver;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.platform.loading.RxModuleAnalysis.RxExportEntry;

/**
 * @author peter.gazdik
 */
/* package */ class RxExportResolver implements ContractSpaceResolver {

	private final Map<Class<? extends RxExportContract>, RxExportEntry> exports;

	public RxExportResolver(Map<Class<? extends RxExportContract>, RxExportEntry> exports) {
		this.exports = exports;
	}

	@Override
	public ContractResolution resolveContractSpace(Class<? extends WireSpace> contractClass) {
		if (!contractClass.isInterface())
			return null;

		RxExportEntry entry = exports.get(contractClass);
		if (entry == null)
			return null;

		WireSpace space = resolveSpace(entry, contractClass);

		return f -> space;
	}

	private WireSpace resolveSpace(RxExportEntry entry, Class<? extends WireSpace> contractClass) {
		if (entry.space != null)
			return entry.space;

		if (entry.moduleWireContext == null)
			throw new IllegalStateException("Cannot resolve contract " + contractClass.getName() + ", exposed by module " + entry.module.moduleName()
					+ ", as that module is not loaded.");

		entry.space = (RxExportContract) entry.moduleWireContext.contract(contractClass);

		return entry.space;
	}

}
