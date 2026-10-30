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
package hiconic.rx.platform.processing.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.braintribe.cfg.Required;
import com.braintribe.execution.ExtendedScheduledThreadPoolExecutor;
import com.braintribe.thread.api.ThreadContextScoping;

/**
 * Transfers the submitting thread's context to scheduled invocations. The execution library's scheduled executor
 * transfers context for {@link #execute(Runnable)}, but scheduled and periodic submissions bypass that path.
 */
public class ContextTransferringScheduledThreadPoolExecutor extends ExtendedScheduledThreadPoolExecutor {
	private ThreadContextScoping threadContextScoping;

	public ContextTransferringScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
		super(corePoolSize, threadFactory);
	}

	@Required
	public void setThreadContextScoping(ThreadContextScoping threadContextScoping) {
		this.threadContextScoping = threadContextScoping;
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return super.schedule(threadContextScoping.bindContext(command), delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return super.schedule(threadContextScoping.bindContext(callable), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return super.scheduleAtFixedRate(threadContextScoping.bindContext(command), initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return super.scheduleWithFixedDelay(threadContextScoping.bindContext(command), initialDelay, delay, unit);
	}
}
