// ============================================================================
package hiconic.rx.explorer.processing.platformreflection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.braintribe.model.resource.Resource;

import hiconic.rx.reflection.model.api.Healthz;
import hiconic.rx.reflection.model.api.PackagingInformation;

public class DiagnosticPackageContext {

	protected String threadDump;
	protected String platformReflectionJson;
	protected String hotThreads;
	protected Healthz healthz;
	protected String processesJson;
	protected File heapDump;
	protected String heapDumpFilename;
	protected File logs;
	protected String logsFilename;
	protected PackagingInformation packagingInformation;
	protected Resource setupDescriptorResource;
	protected File configurationFolderAsZip;
	protected String configurationFolderAsZipFilename;
	protected File modulesFolderAsZip;
	protected String modulesFolderAsZipFilename;
	protected File sharedStorageAsZip;
	protected String sharedStorageAsZipFilename;
	protected File accessDataFolderAsZip;
	protected String accessDataFolderAsZipFilename;
	protected String setupAssetsAsJson;
	protected List<String> errors = Collections.synchronizedList(new ArrayList<>());

}
