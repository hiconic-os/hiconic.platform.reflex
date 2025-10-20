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
package hiconic.rx.explorer.processing.servlet.about.expert;

import java.io.InputStream;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.bootstrapping.TribefireRuntime;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;

import hiconic.rx.explorer.processing.servlet.about.AboutRxServlet;
import hiconic.rx.reflection.model.api.CollectDiagnosticPackages;
import hiconic.rx.reflection.model.api.DiagnosticPackages;
import jakarta.servlet.http.HttpServletResponse;

public class DiagnosticMultinode {

	private static Logger logger = Logger.getLogger(DiagnosticMultinode.class);

	protected Evaluator<ServiceRequest> requestEvaluator;

	public void processDiagnosticPackageRequest(HttpServletResponse resp, String type, String userSessionId) throws Exception {

		logger.debug(() -> "Sending a request to return diagnostic packages to all instances with session " + userSessionId);

		CollectDiagnosticPackages request = CollectDiagnosticPackages.T.create();
		request.setIncludeLogs(true);

		String includeDcsa = TribefireRuntime.getProperty("TRIBEFIRE_DIAGNOSTIC_PACKAGE_INCLUDE_DCSA_BINARIES", "true");
		if (includeDcsa != null && includeDcsa.equalsIgnoreCase("false")) {
			logger.debug(() -> "TRIBEFIRE_DIAGNOSTIC_PACKAGE_INCLUDE_DCSA_BINARIES is set to false. Excluding shared storage binaries.");
			request.setExcludeSharedStorageBinaries(true);
		}

		String timeoutInMsString = TribefireRuntime.getProperty("TRIBEFIRE_DIAGNOSTIC_PACKAGE_TIMEOUT", "600000");
		if (!StringTools.isBlank(timeoutInMsString)) {
			long timeoutInMs = Long.parseLong(timeoutInMsString);
			request.setWaitTimeoutInMs(timeoutInMs);
		}

		if (type.equalsIgnoreCase(AboutRxServlet.TYPE_DIAGNOSTICPACKAGEEXTENDED)) {
			request.setIncludeHeapDump(true);
		}
		DiagnosticPackages dps = request.eval(requestEvaluator).get();
		if (dps != null) {

			Resource resource = dps.getDiagnosticPackages();

			resp.setContentType(resource.getMimeType());
			resp.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", resource.getName()));

			try (InputStream in = resource.openStream()) {
				IOTools.pump(in, resp.getOutputStream(), 0xffff);
			}

		}

		logger.debug(() -> "Done with processing a request to create a diagnostic package.");
	}

	@Configurable
	@Required
	public void setRequestEvaluator(Evaluator<ServiceRequest> requestEvaluator) {
		this.requestEvaluator = requestEvaluator;
	}

}
