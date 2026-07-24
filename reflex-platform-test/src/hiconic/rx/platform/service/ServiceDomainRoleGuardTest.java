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
package hiconic.rx.platform.service;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.result.MulticastResponse;

import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.module.api.service.PlatformServiceDomains;
import hiconic.rx.platform.test.PlatformTestDomains;
import hiconic.rx.test.common.AbstractRxTest;

public class ServiceDomainRoleGuardTest extends AbstractRxTest {

	@Test
	public void internalDomainRejectsExternalContextAndAcceptsSystemContext() {
		Assertions.assertThat(platformContract.serviceProcessing().serviceDomains().internal().domainId())
				.isEqualTo(PlatformServiceDomains.internal.name());
		Assertions.assertThat(platformContract.serviceProcessing().serviceDomains().internal().allowedRoles())
				.containsExactly("internal");

		Maybe<? extends MulticastResponse> externalResult = multicastRequest().eval(evaluator).getReasoned();
		Assertions.assertThat(externalResult.isUnsatisfiedBy(Forbidden.T)).isTrue();

		Maybe<? extends MulticastResponse> internalResult = multicastRequest()
				.eval(platformContract.serviceProcessing().systemEvaluator())
				.getReasoned();
		Assertions.assertThat(internalResult.isSatisfied()).isTrue();
	}

	private MulticastRequest multicastRequest() {
		FileSystemSource source = FileSystemSource.T.create();
		source.setPath("non/existent");

		GetResourcePayload payload = GetResourcePayload.T.create();
		payload.setDomainId(PlatformTestDomains.resources.name());
		payload.setResourceSource(source);

		InstanceId addressee = InstanceId.T.create();
		MulticastRequest request = MulticastRequest.T.create();
		request.setAddressee(addressee);
		request.setServiceRequest(payload);
		return request;
	}
}
