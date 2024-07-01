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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.platform.loading.RxModuleAnalysis.RxExportEntry;

/**
 * @author peter.gazdik
 */
public class RxModuleAnalyzer {

	private final List<RxModule<?>> rxModules;
	private final RxModuleAnalysis analysis = new RxModuleAnalysis();

	public static RxModuleAnalysis analyze(List<RxModule<?>> rxModules) {
		return new RxModuleAnalyzer(rxModules).analyze();
	}

	private RxModuleAnalyzer(List<RxModule<?>> rxModules) {
		this.rxModules = rxModules;
	}

	private RxModuleAnalysis analyze() {
		createNodes();
		determineDependencies();
		failIfCycles();

		return analysis;
	}

	private void createNodes() {
		for (RxModule<?> rxModule : rxModules)
			createNode(rxModule);
	}

	// TODO find all missing imports
	private void determineDependencies() {
		for (RxModuleNode node : analysis.nodes.values())
			for (Class<? extends RxExportContract> importedContract : node.imports)
				determinekDependency(node, importedContract);
	}

	private void determinekDependency(RxModuleNode node, Class<? extends RxExportContract> importedContract) {
		RxExportEntry exportEntry = analysis.exports.get(importedContract);
		if (exportEntry == null)
			throw new IllegalStateException("Cannot import " + importedContract.getClass().getName() + " to module "
					+ node.module.getClass().getName() + " because it is not exported by any module present.");

		RxModuleNode exporterNode = analysis.nodes.get(exportEntry.module);
		node.dependencies.add(exporterNode);
	}

	private void createNode(RxModule<?> rxModule) {
		RxModuleNode node = new RxModuleNode(rxModule, importsOf(rxModule));
		analysis.nodes.put(rxModule, node);

		analysis.setCurrentNode(node);
		rxModule.bindExports(analysis);
	}

	private static Set<Class<? extends RxExportContract>> importsOf(RxModule<?> rxModule) {
		Set<Class<? extends RxExportContract>> exportContracts = new LinkedHashSet<>();

		visit(rxModule.moduleSpaceClass(), exportContracts, new HashSet<>());

		return exportContracts;
	}

	private static void visit(Class<? extends WireSpace> space, Set<Class<? extends RxExportContract>> result,
			Set<Class<? extends WireSpace>> visited) {

		if (!visited.add(space))
			return;

		if (!space.isAnnotationPresent(Managed.class))
			return;

		for (Field field : space.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Import.class))
				continue;

			Class<?> type = field.getType();

			if (type.isInterface()) {
				if (RxExportContract.class.isAssignableFrom(type))
					result.add((Class<? extends RxExportContract>) type);

			} else if (type.isAssignableFrom(WireSpace.class)) {
				Class<? extends WireSpace> importedSpaceClass = (Class<? extends WireSpace>) type;

				visit(importedSpaceClass, result, visited);
			}
		}
	}

	private void failIfCycles() {
		Set<RxModuleNode> trail = new LinkedHashSet<>();
		Set<RxModuleNode> allVisited = new HashSet<>();

		for (RxModuleNode node : analysis.nodes.values())
			checkCycles(node, trail, allVisited);
	}

	private void checkCycles(RxModuleNode node, Set<RxModuleNode> trail, Set<RxModuleNode> allVisited) {
		if (!trail.add(node))
			cycleDetected(node, trail);

		try {
			if (!allVisited.add(node))
				return;

			for (RxModuleNode dependency : node.dependencies)
				checkCycles(dependency, trail, allVisited);

		} finally {
			trail.remove(node);
		}
	}

	private void cycleDetected(RxModuleNode node, Set<RxModuleNode> trailSet) {
		List<RxModuleNode> trail = new ArrayList<>(trailSet);
		List<RxModuleNode> cycle = trail.subList(trail.indexOf(node), trail.size());

		StringBuilder sb = new StringBuilder("Dependency cycle found: ");

		for (RxModuleNode rxModuleNode : cycle) {
			sb.append(rxModuleNode.module.getClass().getName());
			sb.append(" -> ");
		}

		sb.append(node.module.getClass().getName());

		throw new IllegalStateException(sb.toString());
	}

}
