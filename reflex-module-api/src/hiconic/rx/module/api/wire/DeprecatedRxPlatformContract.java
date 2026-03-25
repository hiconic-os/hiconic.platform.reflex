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
package hiconic.rx.module.api.wire;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.CharacterMarshaller;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.module.api.state.RxApplicationStateManager;

/**
 * These methods were originally put on {@link RxPlatformContract}, which was then refactored to be purely a catalog of other contracts available by
 * the platform.
 */
@Deprecated
public interface DeprecatedRxPlatformContract extends WireSpace {

	/** @deprecated use {@link RxServiceProcessingContract#evaluator()} */
	@Deprecated
	Evaluator<ServiceRequest> evaluator();

	/** @deprecated use {@link RxServiceProcessingContract#systemEvaluator()} */
	@Deprecated
	Evaluator<ServiceRequest> systemEvaluator();

	/** @deprecated use {@link RxServiceProcessingContract#evaluator(AttributeContext)} */
	@Deprecated
	Evaluator<ServiceRequest> evaluator(AttributeContext attributeContext);

	/** @deprecated use {@link RxServiceProcessingContract#serviceDomains()} */
	@Deprecated
	ServiceDomains serviceDomains();

	/** @deprecated use {@link RxAuthContract#systemAttributeContextSupplier()} */
	@Deprecated
	Supplier<AttributeContext> systemAttributeContextSupplier();

	/** @deprecated use {@link RxConfigurationContract#configuredModels()} */
	@Deprecated
	ConfiguredModels configuredModels();

	/** @deprecated use {@link RxConfigurationContract#readConfig(EntityType)} */
	@Deprecated
	<C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType);

	/** @deprecated use {@link RxMarshallingContract#marshallers()} */
	@Deprecated
	MarshallerRegistry marshallers();
	/** @deprecated use {@link RxMarshallingContract#jsonMarshaller()} */
	@Deprecated
	CharacterMarshaller jsonMarshaller();
	/** @deprecated use {@link RxMarshallingContract#yamlMarshaller()} */
	@Deprecated
	CharacterMarshaller yamlMarshaller();
	/** @deprecated use {@link RxMarshallingContract#binMarshaller()} */
	@Deprecated
	Marshaller binMarshaller();

	/** @deprecated use {@link RxApplicationContract#logManager()} */
	@Deprecated
	RxLogManager logManager();

	/** @deprecated use {@link RxApplicationContract#stateManager()} */
	@Deprecated
	RxApplicationStateManager stateManager();

	/** @deprecated use {@link RxApplicationContract#applicationName()} */
	@Deprecated
	String applicationName();

	/** @deprecated use {@link RxApplicationContract#applicationId()} */
	@Deprecated
	String applicationId();

	/** @deprecated use {@link RxApplicationContract#nodeId()} */
	@Deprecated
	String nodeId();

	/** @deprecated use {@link RxApplicationContract#instanceId()} */
	@Deprecated
	InstanceId instanceId();

	/** @deprecated use {@link RxExecutionContract#threadRenamer()} */
	@Deprecated
	ThreadRenamer threadRenamer();

	/** @deprecated use {@link RxExecutionContract#executorService()} */
	@Deprecated
	ExecutorService executorService();

	/** @deprecated use {@link RxExecutionContract#scheduledExecutorService()} */
	@Deprecated
	ScheduledExecutorService scheduledExecutorService();

	/** @deprecated use {@link RxExecutionContract#threadContextScoping()} */
	@Deprecated
	DeferringThreadContextScoping threadContextScoping();

	/** @deprecated use {@link RxExecutionContract#taskScheduler()} */
	@Deprecated
	TaskScheduler taskScheduler();

	/** @deprecated use {@link RxExecutionContract#workerManager()} */
	@Deprecated
	WorkerManager workerManager();

}