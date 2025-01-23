package hiconic.rx.push.api;

/**
 * Informs {@link PushChannelLifecycleListener listeners} about PushConnections being established or closed
 */
public interface PushChannelLifecyclePublisher {
	void addListener(PushChannelLifecycleListener listener);
    void removeListener(PushChannelLifecycleListener listener);
}
