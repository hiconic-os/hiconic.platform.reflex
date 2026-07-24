// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
// ============================================================================
package hiconic.rx.platform.processing.push;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import hiconic.rx.push.api.PushChannel;
import hiconic.rx.push.api.PushChannelLifecycleListener;
import hiconic.rx.push.api.PushChannelLifecyclePublisher;

public class PushChannelLifecycleHub implements PushChannelLifecyclePublisher {
	private final List<PushChannelLifecycleListener> listeners = new CopyOnWriteArrayList<>();

	@Override
	public void addListener(PushChannelLifecycleListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(PushChannelLifecycleListener listener) {
		listeners.remove(listener);
	}

	public void notifyConnectionEstablished(PushChannel channel) {
		for (PushChannelLifecycleListener listener : listeners)
			listener.onConnectionEstablished(channel);
	}

	public void notifyConnectionClosed(PushChannel channel) {
		for (PushChannelLifecycleListener listener : listeners)
			listener.onConnectionClosed(channel);
	}
}
