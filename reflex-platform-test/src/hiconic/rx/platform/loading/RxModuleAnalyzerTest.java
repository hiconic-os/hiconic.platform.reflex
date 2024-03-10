package hiconic.rx.platform.loading;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.platform.loading.RxModuleAnalysis.RxExportEntry;
import hiconic.rx.platform.loading.samples.api.ApiContract;
import hiconic.rx.platform.loading.samples.cycle_a.CycleAModule;
import hiconic.rx.platform.loading.samples.cycle_b.CycleBModule;
import hiconic.rx.platform.loading.samples.exporter.ExportingModule;
import hiconic.rx.platform.loading.samples.importer.ImportingModule;

/**
 * @author peter.gazdik
 */
public class RxModuleAnalyzerTest {

	private RxModuleAnalysis analysis = null;

	@Test
	public void simpleImportExport() throws Exception {
		analyze(ExportingModule.INSTANCE, ImportingModule.INSTANCE);

		assertDependency(ImportingModule.INSTANCE, ExportingModule.INSTANCE, ApiContract.class);
	}

	@Test(expected = IllegalStateException.class)
	public void cycle() throws Exception {
		analyze(CycleAModule.INSTANCE, CycleBModule.INSTANCE);
	}

	private void analyze(RxModule<?>... modules) {
		analysis = RxModuleAnalyzer.analyze(asList(modules));
	}

	private void assertDependency(RxModule<?> importer, RxModule<?> exporter, Class<? extends RxExportContract> contractClass) {
		RxModuleNode importerNode = analysis.nodes.get(importer);
		assertThat(importerNode).isNotNull();

		RxModuleNode exporterNode = analysis.nodes.get(exporter);
		assertThat(exporterNode).isNotNull();

		assertThat(importerNode.dependencies).contains(exporterNode);

		assertThat(importerNode.imports).contains(contractClass);

		RxExportEntry contractEntry = analysis.exports.get(contractClass);
		assertThat(contractEntry).isNotNull();

		assertThat(contractEntry.module).isEqualTo(exporter);
		assertThat(contractEntry.spaceClass).isAssignableTo(contractClass);
	}

}
