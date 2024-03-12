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
