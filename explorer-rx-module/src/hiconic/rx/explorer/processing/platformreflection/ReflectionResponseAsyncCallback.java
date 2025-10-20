// ============================================================================
package hiconic.rx.explorer.processing.platformreflection;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.resource.Resource;
import com.braintribe.processing.async.api.AsyncCallback;
import com.braintribe.utils.IOTools;

import hiconic.rx.reflection.model.api.AccessDataFolder;
import hiconic.rx.reflection.model.api.ConfigurationFolder;
import hiconic.rx.reflection.model.api.Healthz;
import hiconic.rx.reflection.model.api.PackagingInformation;
import hiconic.rx.reflection.model.api.PlatformReflection;
import hiconic.rx.reflection.model.api.PlatformReflectionJson;
import hiconic.rx.reflection.model.api.ProcessesJson;
import hiconic.rx.reflection.model.jvm.HeapDump;
import hiconic.rx.reflection.model.jvm.HotThreads;
import hiconic.rx.reflection.model.jvm.ThreadDump;
import hiconic.rx.reflection.model.system.SystemInfo;

public abstract class ReflectionResponseAsyncCallback<T extends GenericEntity> implements AsyncCallback<T> {

	private static Logger logger = Logger.getLogger(ReflectionResponseAsyncCallback.class);

	protected CountDownLatch countdown;
	protected PlatformReflection reflection;
	protected DiagnosticPackageContext diagnosticPackageContext;

	public ReflectionResponseAsyncCallback(PlatformReflection reflection, CountDownLatch countdown) {
		this.reflection = reflection;
		this.countdown = countdown;
	}

	public ReflectionResponseAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
		this.diagnosticPackageContext = diagnosticPackageContext;
		this.countdown = countdown;
	}

	@Override
	public void onFailure(Throwable t) {
		countdown.countDown();
		logger.error("Error while waiting for a PlatformReflectionResponse", t);
		try {
			String trace = Exceptions.stringify(t);
			String name = getClass().getSimpleName();
			diagnosticPackageContext.errors.add(name + "\n" + trace);
		} catch (Exception e) {
			logger.debug("Could not add the error to the context", e);
		}
	}

	public static class SystemInfoAsyncCallback extends ReflectionResponseAsyncCallback<SystemInfo> {
		public SystemInfoAsyncCallback(PlatformReflection reflection, CountDownLatch countdown) {
			super(reflection, countdown);
		}
		@Override
		public void onSuccess(SystemInfo future) {
			logger.debug(() -> "Received a SystemInfo response asynchronously.");
			reflection.setSystemInfo(future);
			countdown.countDown();
		}
	}

	public static class ThreadDumpAsyncCallback extends ReflectionResponseAsyncCallback<ThreadDump> {
		public ThreadDumpAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(ThreadDump future) {
			try {
				logger.debug(() -> "Received a ThreadDump response asynchronously.");
				diagnosticPackageContext.threadDump = future.getThreadDump();
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class PlatformReflectionJsonAsyncCallback extends ReflectionResponseAsyncCallback<PlatformReflectionJson> {
		public PlatformReflectionJsonAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(PlatformReflectionJson future) {
			try {
				logger.debug(() -> "Received a PlatformReflectionJson response asynchronously.");
				diagnosticPackageContext.platformReflectionJson = future.getPlatformReflectionJson();
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class HotThreadsAsyncCallback extends ReflectionResponseAsyncCallback<HotThreads> {
		public HotThreadsAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(HotThreads future) {
			try {
				logger.debug(() -> "Received a HotThreads response asynchronously.");
				diagnosticPackageContext.hotThreads = PlatformReflectionTools.toString(future);
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class CollectHealthzAsyncCallback extends ReflectionResponseAsyncCallback<Healthz> {
		public CollectHealthzAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(Healthz healthz) {
			try {
				logger.debug(() -> "Received a Healthz response asynchronously.");
				diagnosticPackageContext.healthz = healthz;
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class CollectPackagingInformationAsyncCallback extends ReflectionResponseAsyncCallback<PackagingInformation> {
		public CollectPackagingInformationAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(PackagingInformation pi) {
			try {
				logger.debug(() -> "Received a PackagingInformation response asynchronously.");
				diagnosticPackageContext.packagingInformation = pi;
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class CollectSetupDescriptorAsyncCallback extends ReflectionResponseAsyncCallback<Resource> {
		public CollectSetupDescriptorAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(Resource setupDescriptorResource) {
			try {
				logger.debug(() -> "Received a PackagingInformation response asynchronously.");
				diagnosticPackageContext.setupDescriptorResource = setupDescriptorResource;
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class CollectConfigurationFolderAsyncCallback extends ReflectionResponseAsyncCallback<ConfigurationFolder> {
		public CollectConfigurationFolderAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(ConfigurationFolder cf) {
			try {
				logger.debug(() -> "Received a ConfigurationFolder response asynchronously.");

				Resource resource = cf.getConfigurationFolderAsZip();
				if (resource != null) {
					File tempFile = File.createTempFile(resource.getName(), ".tmp");
					tempFile.delete();
					try (InputStream in = resource.openStream()) {
						IOTools.inputToFile(in, tempFile);
					}
					diagnosticPackageContext.configurationFolderAsZip = tempFile;
					diagnosticPackageContext.configurationFolderAsZipFilename = resource.getName();
				}
			} catch (Exception e) {
				logger.error("Error while trying to include the configuration folder as a ZIP in the diagnostic package.", e);
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class CollectAccessDataFolderAsyncCallback extends ReflectionResponseAsyncCallback<AccessDataFolder> {
		public CollectAccessDataFolderAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(AccessDataFolder adf) {
			try {
				logger.debug(() -> "Received a AccessDataFolder response asynchronously.");

				Resource resource = adf.getAccessDataFolderAsZip();
				if (resource != null) {
					File tempFile = File.createTempFile(resource.getName(), ".tmp");
					tempFile.delete();
					try (InputStream in = resource.openStream()) {
						IOTools.inputToFile(in, tempFile);
					}
					diagnosticPackageContext.accessDataFolderAsZip = tempFile;
					diagnosticPackageContext.accessDataFolderAsZipFilename = resource.getName();
				}
			} catch (Exception e) {
				logger.error("Error while trying to include the accessdata folder ZIP in the diagnostic package.", e);
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class ProcessesJsonAsyncCallback extends ReflectionResponseAsyncCallback<ProcessesJson> {
		public ProcessesJsonAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(ProcessesJson future) {
			try {
				logger.debug(() -> "Received a ProcessesJson response asynchronously.");
				diagnosticPackageContext.processesJson = future.getProcessesJson();
			} finally {
				countdown.countDown();
			}
		}
	}

	public static class HeapDumpAsyncCallback extends ReflectionResponseAsyncCallback<HeapDump> {
		public HeapDumpAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
			super(diagnosticPackageContext, countdown);
		}
		@Override
		public void onSuccess(HeapDump future) {
			try {
				logger.debug(() -> "Received a HeapDump response asynchronously.");

				Resource resource = future.getHeapDump();
				File tempFile = File.createTempFile(resource.getName(), ".tmp");
				tempFile.delete();
				try (InputStream in = resource.openStream()) {
					IOTools.inputToFile(in, tempFile);
				}
				diagnosticPackageContext.heapDump = tempFile;
				diagnosticPackageContext.heapDumpFilename = resource.getName();
			} catch (Exception e) {
				logger.error("Error while trying to include the heap dump in the diagnostic package.", e);
			} finally {
				countdown.countDown();
			}
		}
	}
}
