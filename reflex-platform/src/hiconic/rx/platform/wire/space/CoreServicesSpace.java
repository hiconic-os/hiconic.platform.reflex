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
package hiconic.rx.platform.wire.space;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.CharacterMarshaller;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.module.api.wire.RxExecutionContract;
import hiconic.rx.module.api.wire.RxMarshallingContract;
import hiconic.rx.module.api.wire.RxServiceProcessingContract;
import hiconic.rx.platform.wire.contract.CoreServicesContract;

@Managed
public class CoreServicesSpace implements CoreServicesContract {

	@Import
	private RxAuthContract auth;

	@Import
	private RxConfigurationSpace configuration;

	@Import
	private RxExecutionContract execution;

	@Import
	private RxMarshallingContract marshalling;

	@Import
	private RxServiceProcessingContract serviceProcessing;

	@Override
	public Evaluator<ServiceRequest> evaluator() {
		return serviceProcessing.evaluator();
	}

	@Override
	public Evaluator<ServiceRequest> systemEvaluator() {
		return serviceProcessing.systemEvaluator();
	}

	@Override
	public Evaluator<ServiceRequest> evaluator(AttributeContext attributeContext) {
		return serviceProcessing.evaluator(attributeContext);
	}

	@Override
	public Supplier<AttributeContext> systemAttributeContextSupplier() {
		return auth.systemAttributeContextSupplier();
	}

	@Override
	public ConfiguredModels configuredModels() {
		return configuration.configuredModels();
	}

	@Override
	public ServiceDomains serviceDomains() {
		return serviceProcessing.serviceDomains();
	}

	@Override
	public DeferringThreadContextScoping threadContextScoping() {
		return execution.threadContextScoping();
	}

	@Override
	public MarshallerRegistry marshallers() {
		return marshalling.marshallers();
	}

	@Override
	public CharacterMarshaller jsonMarshaller() {
		return marshalling.jsonMarshaller();
	}

	@Override
	public CharacterMarshaller yamlMarshaller() {
		return marshalling.yamlMarshaller();
	}

	@Override
	public Marshaller binMarshaller() {
		return marshalling.binMarshaller();
	}

	@Override
	public ThreadRenamer threadRenamer() {
		return execution.threadRenamer();
	}

	@Override
	public ExecutorService executorService() {
		return execution.executorService();
	}

	@Override
	public ScheduledExecutorService scheduledExecutorService() {
		return execution.scheduledExecutorService();
	}

	@Override
	public TaskScheduler taskScheduler() {
		return execution.taskScheduler();
	}

}
