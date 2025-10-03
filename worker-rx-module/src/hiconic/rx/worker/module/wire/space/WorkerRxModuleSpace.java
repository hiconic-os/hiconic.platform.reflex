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
package hiconic.rx.worker.module.wire.space;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import com.braintribe.execution.virtual.VirtualThreadExecutor;
import com.braintribe.execution.virtual.VirtualThreadExecutorBuilder;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.leadership.api.LeadershipContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.worker.api.WorkerContract;
import hiconic.rx.worker.module.processing.ClusterAwareWorkerManager;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WorkerRxModuleSpace implements RxModuleContract, WorkerContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private LeadershipContract leadership;

	@Override
	public void onApplicationReady() {
		manager().start();
	}

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		configurator.setWorkerManager(manager());
	}

	@Override
	@Managed
	public ClusterAwareWorkerManager manager() {
		ClusterAwareWorkerManager bean = new ClusterAwareWorkerManager();
		bean.setLeadershipManagerSupplier(leadership::leadershipManager);
		bean.setExecutorService(threadPool());
		bean.setApplicationId(platform.applicationId());
		return bean;
	}

	@Override
	@Managed
	public ExecutorService threadPool() {
		VirtualThreadExecutor bean = VirtualThreadExecutorBuilder.newPool() //
				.concurrency(Integer.MAX_VALUE) //
				.threadNamePrefix("rx.worker-") //
				.description("Rx Worker Thread-Pool") //
				.interruptThreadsOnShutdown(true) //
				.terminationTimeout(Duration.ofSeconds(30)) //
				.build();
		return bean;
	}

}