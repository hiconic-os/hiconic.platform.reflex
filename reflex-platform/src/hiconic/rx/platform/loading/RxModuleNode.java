package hiconic.rx.platform.loading;

import java.util.LinkedHashSet;
import java.util.Set;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.RxModuleAnalysis.RxExportEntry;

/**
 * @author peter.gazdik
 */
/* package */ class RxModuleNode {

	public final RxModule<RxModuleContract> module;
	public final Set<RxExportEntry> exports = new LinkedHashSet<>();
	public final Set<Class<? extends RxExportContract>> imports;
	public final Set<RxModuleNode> dependencies = new LinkedHashSet<>();


	public RxModuleNode(RxModule<?> module, Set<Class<? extends RxExportContract>> imports) {
		this.module = (RxModule<RxModuleContract>) module;
		this.imports = imports;
	}

	private int index = 0;

	public int getIndex() {
		if (index != 0)
			return index;

		if (dependencies.isEmpty())
			return index = 1;

		return index = 1 + dependencies.stream() //
				.mapToInt(RxModuleNode::getIndex) //
				.max() //
				.getAsInt();
	}
}
