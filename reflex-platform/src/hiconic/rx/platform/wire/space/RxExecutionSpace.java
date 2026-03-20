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

import static com.braintribe.wire.api.util.Lists.list;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.common.concurrent.TaskSchedulerImpl;
import com.braintribe.execution.ExtendedScheduledThreadPoolExecutor;
import com.braintribe.execution.ExtendedThreadPoolExecutor;
import com.braintribe.execution.ThreadPoolBuilder;
import com.braintribe.execution.virtual.CountingVirtualThreadFactory;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.provider.Box;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.thread.impl.ThreadContextScopingImpl;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxExecutionContract;
import hiconic.rx.platform.processing.thread.AttributeContextThreadContextScope;
import hiconic.rx.platform.processing.worker.BasicRxWorkerManager;
import hiconic.rx.platform.processing.worker.BasicWorkerAspectRegistry;

@Managed
public class RxExecutionSpace implements RxExecutionContract {

	@Import
	private RxApplicationSpace application;

	@Override
	@Managed
	public ThreadRenamer threadRenamer() {
		ThreadRenamer bean = new ThreadRenamer(true);
		return bean;
	}

	@Override
	@Managed
	public ExecutorService executorService() {
		ExtendedThreadPoolExecutor bean = ThreadPoolBuilder.newPool() //
				.poolSize(5, Integer.MAX_VALUE) //
				.workQueue(new SynchronousQueue<>()) //
				.threadNamePrefix("rx.platform-") //
				.description("Rx Platform Thread-Pool") //
				.build();
		return bean;
	}

	@Override
	@Managed
	public ExtendedScheduledThreadPoolExecutor scheduledExecutorService() {
		ExtendedScheduledThreadPoolExecutor bean = new ExtendedScheduledThreadPoolExecutor( //
				5, //
				new CountingVirtualThreadFactory("rx.scheduled-") //
		);
		bean.setAddThreadContextToNdc(true);
		bean.allowCoreThreadTimeOut(true);
		bean.setDescription("Rx Platform Scheduled Thread-Pool");

		return bean;
	}

	@Override
	@Managed
	public TaskScheduler taskScheduler() {
		TaskSchedulerImpl bean = new TaskSchedulerImpl();
		bean.setName("Rx Platform-Task-Scheduler");
		bean.setExecutor(scheduledExecutorService());

		return bean;
	}

	@Override
	@Managed
	public DeferringThreadContextScoping threadContextScoping() {
		ThreadContextScopingImpl bean = new ThreadContextScopingImpl();
		bean.setScopeSuppliers(list(AttributeContextThreadContextScope.SUPPLIER));
		return bean;
	}

	@Override
	public WorkerManager workerManager() {
		return workerManagerHolder().value;
	}

	@Managed
	public Box<WorkerManager> workerManagerHolder() {
		Box<WorkerManager> bean = Box.of(defaultWorkerManager());
		return bean;
	}

	private BasicRxWorkerManager defaultWorkerManager() {
		BasicRxWorkerManager result = new BasicRxWorkerManager();
		result.setExecutorService(executorService());
		result.setApplicationId(application.applicationId());
		return result;
	}

	@Managed
	public ConfigurableWorkerAspectRegistry workerAspectRegistry() {
		BasicWorkerAspectRegistry bean = new BasicWorkerAspectRegistry();
		return bean;
	}

}