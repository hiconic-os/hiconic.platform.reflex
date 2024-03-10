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
/* package */ class ImportResolver implements ContractSpaceResolver {

	private final Map<Class<? extends RxExportContract>, RxExportEntry> exports;

	public ImportResolver(Map<Class<? extends RxExportContract>, RxExportEntry> exports) {
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
			throw new IllegalStateException("Cannot resolve contract " + contractClass.getName() + ", expored by module " + entry.module.moduleName()
					+ ", as that module is not loaded.");

		return entry.moduleWireContext.contract(contractClass);
	}

}
