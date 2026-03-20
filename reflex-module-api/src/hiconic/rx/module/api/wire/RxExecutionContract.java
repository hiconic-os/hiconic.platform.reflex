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

import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.wire.api.space.WireSpace;

/**
 * Contract for threading and task scheduling/execution.
 */
public interface RxExecutionContract extends WireSpace {

	/** The one ThreadRenamer used in the entire application. */
	ThreadRenamer threadRenamer();

	/** General purpose thread pool. */
	ExecutorService executorService();

	ScheduledExecutorService scheduledExecutorService();

	DeferringThreadContextScoping threadContextScoping();

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
