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
package hiconic.platform.reflex.websocket_server.processing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import hiconic.rx.push.api.PushChannel;
import hiconic.rx.push.api.PushChannelLifecycleListener;
import hiconic.rx.push.api.PushChannelLifecyclePublisher;

public class PushChannelLifecycleHub implements PushChannelLifecyclePublisher {
	//private List<PushChannelLifecycleListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private List<PushChannelLifecycleListener> listeners = new CopyOnWriteArrayList<>();

	@Override
	public void addListener(PushChannelLifecycleListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(PushChannelLifecycleListener listener) {
		listeners.remove(listener);
	}
	
	public void notifyConnectionEstablished(PushChannel channel) {
		for (PushChannelLifecycleListener listener: listeners) {
			listener.onConnectionEstablished(channel);
		}
	}

	public void notifyConnectionClosed(PushChannel channel) {
		for (PushChannelLifecycleListener listener: listeners) {
			listener.onConnectionClosed(channel);
		}
	}
}
