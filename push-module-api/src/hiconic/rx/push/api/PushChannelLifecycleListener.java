package hiconic.rx.push.api;

public interface PushChannelLifecycleListener {
	void onConnectionEstablished(PushChannel channel);
    void onConnectionClosed(PushChannel channel);
}
