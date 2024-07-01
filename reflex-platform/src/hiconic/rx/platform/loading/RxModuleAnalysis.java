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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.wire.api.context.WireContext;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.RxModuleContract;

/**
 * @author peter.gazdik
 */
public class RxModuleAnalysis implements Exports {

	public final Map<RxModule<?>, RxModuleNode> nodes = new LinkedHashMap<>();

	public final Map<Class<? extends RxExportContract>, RxExportEntry> exports = new HashMap<>();

	private RxModuleNode currentNode;

	public void setCurrentNode(RxModuleNode node) {
		currentNode = node;
	}

	@Override
	public <E extends RxExportContract> void bind(Class<E> contractClass, E spaceInstance) {
		bind(contractClass, null, spaceInstance);
	}

	@Override
	public <E extends RxExportContract, S extends E> void bind(Class<E> contractClass, Class<S> spaceClass) {
		bind(contractClass, spaceClass, null);
	}

	private <E extends RxExportContract, S extends E> void bind(Class<E> contractClass, Class<S> spaceClass, E spaceInstance) {
		RxExportEntry entry = new RxExportEntry(currentNode.module, contractClass, spaceClass, spaceInstance);

		currentNode.exports.add(entry);

		RxExportEntry previousEntry = exports.put(contractClass, entry);

		if (previousEntry != null)
			throw new IllegalStateException("Multiple attempts to export contract" + contractClass + ". Involved modules: "
					+ currentNode.module.getClass().getName() + ", " + previousEntry.module.getClass().getName());
	}

	public static class RxExportEntry {
		// because we keep these entries in memory, we don't want to store the RxModuleNode with all the temporary information
		public final RxModule<RxModuleContract> module;
		public final Class<? extends RxExportContract> contractClass;
		public final Class<? extends RxExportContract> spaceClass;
		public RxExportContract space;
		public WireContext<?> moduleWireContext;

		public RxExportEntry(RxModule<RxModuleContract> module, Class<? extends RxExportContract> contractClass,
				Class<? extends RxExportContract> spaceClass, RxExportContract space) {
			this.module = module;
			this.contractClass = contractClass;
			this.spaceClass = spaceClass;
			this.space = space;
		}

	}

}
