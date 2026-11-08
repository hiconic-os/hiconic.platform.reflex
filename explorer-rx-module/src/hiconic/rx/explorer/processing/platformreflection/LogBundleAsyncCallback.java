package hiconic.rx.explorer.processing.platformreflection;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import com.braintribe.logging.Logger;
import com.braintribe.model.resource.Resource;
import com.braintribe.processing.async.api.AsyncCallback;
import com.braintribe.utils.IOTools;

/** Adds a log-reflection bundle to a platform diagnostic package. */
public class LogBundleAsyncCallback implements AsyncCallback<Resource> {
	private static final Logger logger = Logger.getLogger(LogBundleAsyncCallback.class);

	private final CountDownLatch countdown;
	private final DiagnosticPackageContext diagnosticPackageContext;

	public LogBundleAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
		this.diagnosticPackageContext = diagnosticPackageContext;
		this.countdown = countdown;
	}

	@Override
	public void onFailure(Throwable throwable) {
		countdown.countDown();
		logger.error("Error while waiting for the log bundle", throwable);
	}

	@Override
	public void onSuccess(Resource resource) {
		try {
			if (resource == null)
				return;
			File tempFile = File.createTempFile(resource.getName(), ".tmp");
			tempFile.delete();
			try (InputStream in = resource.openStream()) {
				IOTools.inputToFile(in, tempFile);
			}
			diagnosticPackageContext.logs = tempFile;
			diagnosticPackageContext.logsFilename = resource.getName();
		} catch (Exception e) {
			logger.error("Error while trying to include logs in the diagnostic package", e);
		} finally {
			countdown.countDown();
		}
	}
}
