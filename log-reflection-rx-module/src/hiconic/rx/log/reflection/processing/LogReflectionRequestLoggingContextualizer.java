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
package hiconic.rx.log.reflection.processing;

import com.braintribe.model.processing.service.api.ServiceRequestContextualizer;
import com.braintribe.model.processing.service.api.ServiceRequestExecution;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.api.QueryLocalLogRecords;
import hiconic.rx.log.reflection.model.api.QueryLogRecords;
import hiconic.rx.module.api.log.RxLogManager;

/** Suppresses routine logs produced while a long-polling log observation request is being processed. */
public class LogReflectionRequestLoggingContextualizer implements ServiceRequestContextualizer {

	private final RxLogManager logManager;

	public LogReflectionRequestLoggingContextualizer(RxLogManager logManager) {
		this.logManager = logManager;
	}

	@Override
	public void contextualize(ServiceRequest request, ServiceRequestExecution execution) throws Exception {
		if (isLongPollingLogRequest(request))
			logManager.suppressRoutineLogging(execution);
		else
			execution.execute();
	}

	static boolean isLongPollingLogRequest(ServiceRequest request) {
		if (request instanceof QueryLogRecords query)
			return query.getWaitMillis() > 0;

		if (request instanceof QueryLocalLogRecords query)
			return query.getWaitMillis() > 0;

		return false;
	}
}
