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
