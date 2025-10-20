// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.explorer.processing.platformreflection;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import com.braintribe.logging.Logger;
import com.braintribe.model.resource.Resource;
import com.braintribe.processing.async.api.AsyncCallback;
import com.braintribe.utils.IOTools;

import hiconic.rx.logs.model.api.Logs;

public class LogsAsyncCallback implements AsyncCallback<Logs> {

	private static Logger logger = Logger.getLogger(LogsAsyncCallback.class);

	protected CountDownLatch countdown;
	protected DiagnosticPackageContext diagnosticPackageContext;

	public LogsAsyncCallback(DiagnosticPackageContext diagnosticPackageContext, CountDownLatch countdown) {
		this.diagnosticPackageContext = diagnosticPackageContext;
		this.countdown = countdown;
	}

	@Override
	public void onFailure(Throwable t) {
		countdown.countDown();
		logger.error("Error while waiting for logs", t);
	}

	@Override
	public void onSuccess(Logs future) {
		try {
			logger.debug("Received an asynchronous Logs response.");

			Resource resource = future.getLog();
			if (resource == null)
				return;

			File tempFile = File.createTempFile(resource.getName(), ".tmp");
			tempFile.delete();
			try (InputStream in = resource.openStream()) {
				IOTools.inputToFile(in, tempFile);
			} 
			diagnosticPackageContext.logs = tempFile;
			diagnosticPackageContext.logsFilename = resource.getName();

		} catch(Exception e) {
			logger.error("Error while trying to include logs in the diagnostic package.", e);

		} finally {
			countdown.countDown();
		}
	}


}
