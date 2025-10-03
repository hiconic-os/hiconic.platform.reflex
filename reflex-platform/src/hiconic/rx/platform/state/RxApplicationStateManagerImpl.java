package hiconic.rx.platform.state;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Reason;

import hiconic.rx.module.api.state.RxApplicationState;
import hiconic.rx.module.api.state.RxApplicationStateManager;

public class RxApplicationStateManagerImpl implements RxApplicationStateManager {
	private volatile RxApplicationState state;
	private Reason deathReason;
	private List<Callable<Reason>> livenessCheckers = new CopyOnWriteArrayList<>();
	private List<Consumer<RxApplicationState>> stateListeners = new CopyOnWriteArrayList<>();
	
	@Override
	public synchronized boolean isLive() {
		if (state != RxApplicationState.dead)
			return false;
		
		Reason deathReason = checkLiveness();
		
		if (deathReason != null)
			setState(state);
	}
	
	private Reason checkLiveness() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isReady() {
		return state == RxApplicationState.started;
	}

	@Override
	public RxApplicationState getApplicationState() {
		return state;
	}

	@Override
	public Reason getDeathReason() {
		return deathReason;
	}
	
	public void setState(RxApplicationState state) {
		this.state = state;
		notifyState();
	}

	@Override
	public void addStateListener(Consumer<RxApplicationState> listener) {
		stateListeners.add(listener);
	}

	@Override
	public void addLivenessChecker(Callable<Reason> checker) {
		livenessCheckers.add(checker);
	}
	
	private void notifyState() {
		RxApplicationState currentState = state;
		
		for (Consumer<RxApplicationState> listener: stateListeners) {
			listener.accept(currentState);
		}
	}
}
