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
package hiconic.rx.security.test.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;

import hiconic.rx.security.model.test.RunSecured;
import hiconic.rx.security.model.test.SecurityTestRequest;

public class SecurityTestProcessor extends AbstractDispatchingServiceProcessor<SecurityTestRequest, Object> {
	@Override
	protected void configureDispatching(DispatchConfiguration<SecurityTestRequest, Object> dispatching) {
		dispatching.registerReasoned(RunSecured.T, this::runSecured);
	}
	
	private Maybe<Object> runSecured(ServiceRequestContext context, RunSecured request) {
		return Maybe.complete(null);
	}
}
