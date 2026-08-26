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

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.model.service.api.CompositeRequest;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.result.MulticastResponse;

import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.model.service.processing.md.InterceptWith;
import hiconic.rx.model.service.processing.md.ProcessWith;
import hiconic.rx.module.api.service.PlatformServiceDomains;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.platform.test.PlatformTestDomains;
import hiconic.rx.platform.test.wire.space.PlatformRxTestModuleSpace;
import hiconic.rx.test.common.AbstractRxTest;

public class ServiceDomainRoleGuardTest extends AbstractRxTest {

	@Test
	public void serviceDomainAliasResolvesWithoutCreatingAnotherDomain() {
		var serviceDomains = platformContract.serviceProcessing().serviceDomains();
		var canonicalDomain = serviceDomains.byId(PlatformTestDomains.resources);

		Assertions.assertThat(serviceDomains.byId(PlatformRxTestModuleSpace.resourcesAlias)).isSameAs(canonicalDomain);
		Assertions.assertThat(canonicalDomain.aliases()).containsExactly(PlatformRxTestModuleSpace.resourcesAlias);
		Assertions.assertThat(serviceDomains.list()).filteredOn(domain -> domain.domainId().equals(PlatformRxTestModuleSpace.resourcesAlias)).isEmpty();
	}

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

		ProcessWith effectiveBinding = platformContract.serviceProcessing().serviceDomains().internal().systemCmdResolver()
				.getMetaData().entityType(MulticastRequest.T).meta(ProcessWith.T).exclusive();
		Assertions.assertThat(effectiveBinding).isNotNull();
		Assertions.assertThat(effectiveBinding.getConflictPriority()).isEqualTo(0d);
	}

	@Test
	public void domainInferenceDoesNotMutateSharedDomainIndex() {
		var serviceDomains = platformContract.serviceProcessing().serviceDomains();
		List<? extends ServiceDomain> domainsBefore = List.copyOf(serviceDomains.listDomains(CompositeRequest.T));

		CompositeRequest request = CompositeRequest.T.create();
		request.setRequests(List.of(multicastRequest()));

		request.eval(platformContract.serviceProcessing().systemEvaluator()).getReasoned();
		Assertions.assertThat(serviceDomains.listDomains(CompositeRequest.T)).containsExactlyElementsOf(domainsBefore);
	}

	@Test
	public void resourcePayloadRequestsUsePlatformFallbackInExplicitDomains() {
		GetResourcePayload request = GetResourcePayload.T.create();
		request.setDomainId(PlatformTestDomains.resources.name());
		FileSystemSource source = FileSystemSource.T.create();
		source.setPath("non/existent");
		request.setResourceSource(source);

		Maybe<?> result = request.eval(evaluator).getReasoned();

		Assertions.assertThat(result.isUnsatisfied()).isTrue();
		Assertions.assertThat(result.whyUnsatisfied().getText()).doesNotContain("No service processor mapped");
	}

	@Test
	public void modeledServiceInterceptorsReflectDeclaredOrder() {
		var configuredInterceptors = platformContract.serviceProcessing().serviceDomains().byId(PlatformTestDomains.resources)
				.systemCmdResolver().getMetaData().entityType(GetResourcePayload.T).meta(InterceptWith.T).list();
		Assertions.assertThat(configuredInterceptors).extracting(InterceptWith::getConflictPriority).containsExactly(2d, 1d);
	}

	private MulticastRequest multicastRequest() {
		GetResourcePayload payload = newResourcePayloadRequest();

		InstanceId addressee = InstanceId.T.create();
		MulticastRequest request = MulticastRequest.T.create();
		request.setAddressee(addressee);
		request.setServiceRequest(payload);
		return request;
	}

	private GetResourcePayload newResourcePayloadRequest() {
		FileSystemSource source = FileSystemSource.T.create();
		source.setPath("non/existent");

		GetResourcePayload payload = GetResourcePayload.T.create();
		payload.setDomainId(PlatformTestDomains.resources.name());
		payload.setResourceSource(source);
		return payload;
	}
}
