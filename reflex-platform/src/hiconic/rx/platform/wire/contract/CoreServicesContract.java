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
package hiconic.rx.platform.wire.contract;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ServiceDomains;

// This is used when integrating into a different platform, e.g. Spring Boot
public interface CoreServicesContract extends WireSpace {

	/** Standard service evaluator. */
	Evaluator<ServiceRequest> evaluator();

	/**
	 * {@link Evaluator} backed by {@link #systemAttributeContextSupplier()}, thus having system user (i.e. full) authorization when evaluating
	 * requests.
	 */
	Evaluator<ServiceRequest> systemEvaluator();

	/** {@link Evaluator} backed by given {@link AttributeContext}, which can e..g. */
	Evaluator<ServiceRequest> evaluator(AttributeContext attributeContext);

	/** {@link AttributeContext} equipped with the system user's {@link UserSession}. */
	Supplier<AttributeContext> systemAttributeContextSupplier();

	ConfiguredModels configuredModels();

	ServiceDomains serviceDomains();

	MarshallerRegistry marshallers();

	/** The one ThreadRenamer used in the entire application. */
	ThreadRenamer threadRenamer();

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

}
