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
import com.braintribe.model.usersession.UserSession;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.module.api.state.RxApplicationStateManager;

/**
 * Wire contract that exposes general features of the Reflex platform, and which can be {@link Import imported} to any Reflex module.
 * <p>
 * Additional contracts available to modules are:
 * <ul>
 * <li>{@link RxPlatformResourcesContract}
 * <li>{@link RxTransientDataContract}
 * <li>{@link RxProcessLaunchContract}
 * </ul>
 * 
 * @see RxExportContract
 * @see EnvironmentPropertiesContract
 * @see SystemPropertiesContract
 */
public interface RxPlatformContract extends WireSpace {

	RxApplicationStateManager stateManager();

	/** Standard request {@link Evaluator}, which has the same authorization as the caller. */
	Evaluator<ServiceRequest> evaluator();

	/**
	 * {@link Evaluator} backed by {@link #systemAttributeContextSupplier()}, thus having system user (i.e. full) authorization when evaluating
	 * requests.
	 * <p>
	 * Equivalent to: {@code this.evaluator(systemAttributeContextSupplier().get())}
	 */
	Evaluator<ServiceRequest> systemEvaluator();

	/** {@link Evaluator} backed by given {@link AttributeContext}, which can e.g. have different authorization. */
	Evaluator<ServiceRequest> evaluator(AttributeContext attributeContext);

	/** {@link AttributeContext} equipped with the system user's {@link UserSession}. */
	Supplier<AttributeContext> systemAttributeContextSupplier();

	/** Returns the {@link ConfiguredModels}. */
	ConfiguredModels configuredModels();

	/** Returns the {@link ServiceDomains}. */
	ServiceDomains serviceDomains();

	DeferringThreadContextScoping threadContextScoping();

	/** Returns the {@link MarshallerRegistry} */
	MarshallerRegistry marshallers();
	CharacterMarshaller jsonMarshaller();
	CharacterMarshaller yamlMarshaller();
	Marshaller binMarshaller();

	/** The name of the application which the platform is hosting given by the applicationName property in META-INF/rx-app.properties */
	String applicationName();

	/** The technical application id. */
	String applicationId();

	/** The nodeId of this instance in distributed systems */
	String nodeId();

	/** Holds the applicationId and nodeId information (not sure why this was introduced). */
	InstanceId instanceId();

	RxLogManager logManager();

	/**
	 * Returns a configuration for the given type or a reason why the configuration could not be retrieved.
	 * <p>
	 * If an explicit configuration cannot be found a default initialized instance of the configType will be returned.
	 */
	<C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType);

	/** The one ThreadRenamer used in the entire application. */
	ThreadRenamer threadRenamer();

	/** General purpose thread pool. */
	ExecutorService executorService();

	ScheduledExecutorService scheduledExecutorService();

	/**
	 * {@link TaskScheduler} for tasks which should be run periodically.
	 * <p>
	 * This scheduler is shut down on server shutdown, but waits for the tasks running at the moment of shutdown to finish.
	 * <p>
	 * The time period as to how long it waits is configurable per task.
	 */
	TaskScheduler taskScheduler();

	WorkerManager workerManager();

}
