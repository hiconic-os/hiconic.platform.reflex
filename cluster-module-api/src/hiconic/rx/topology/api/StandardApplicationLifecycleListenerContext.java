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
package hiconic.rx.topology.api;

import java.util.function.Consumer;

/**
 * <p>
 * The standard {@link ApplicationLifecycleListenerContext} provided upon {@link ApplicationLifecycleListener} events.
 * 
 */
public class StandardApplicationLifecycleListenerContext implements ApplicationLifecycleListenerContext {

	private final String applicationId;
	private final String nodeId;
	private final Consumer<String> unregisterCallback;

	public StandardApplicationLifecycleListenerContext(String applicationId, Consumer<String> unregisterCallback) {
		this(applicationId, null, unregisterCallback);
	}

	public StandardApplicationLifecycleListenerContext(String applicationId, String nodeId, Consumer<String> unregisterCallback) {
		this.applicationId = applicationId;
		this.nodeId = nodeId;
		this.unregisterCallback = unregisterCallback;
	}

	@Override
	public String applicationId() {
		return applicationId;
	}

	@Override
	public String nodeId() {
		return nodeId;
	}

	@Override
	public void unsubscribe() {
		unregisterCallback.accept(applicationId);
	}

}
