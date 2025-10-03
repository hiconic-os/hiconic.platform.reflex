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
package hiconic.rx.explorer.processing.check.base;

import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;

public class BaseVitalityCheckProcessor implements CheckProcessor {

	@Override
	public CheckResult check(ServiceRequestContext requestContext) {

		CheckResult response = CheckResult.T.create();

		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName("Base Check");
		entry.setDetails("Check infrastructure is ok");
		entry.setCheckStatus(CheckStatus.ok);

		response.getEntries().add(entry);

		return response;

	}
}
