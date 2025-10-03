package hiconic.rx.platform.state;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Reason;

import hiconic.rx.module.api.state.RxApplicationLivenessChecker;
import hiconic.rx.module.api.state.RxApplicationState;
import hiconic.rx.module.api.state.RxApplicationStateManager;

public class RxApplicationStateManagerImpl implements RxApplicationStateManager {
	private static final Logger logger = System.getLogger(RxApplicationStateManagerImpl.class.getName());
	private volatile RxApplicationState state = RxApplicationState.initial;
	private Reason deathReason;
	private List<RxApplicationLivenessChecker> livenessCheckers = new CopyOnWriteArrayList<>();
	private List<Consumer<RxApplicationState>> stateListeners = new CopyOnWriteArrayList<>();
	
	@Override
	public synchronized boolean isLive() {
		if (state == RxApplicationState.dead)
			return false;
		
		Reason deathReason = checkLiveness();
		
		if (deathReason == null)
			return true;
		
		
		setDead(deathReason);
		
		return false;
	}
	
	private Reason checkLiveness() {
		List<Reason> errors = new ArrayList<>();
		
		for (RxApplicationLivenessChecker livenessChecker: livenessCheckers) {
			try {
				Reason error = livenessChecker.checkLiveness();
				if (error != null)
					errors.add(error);
			}
			catch (Exception e) {
				/* we do not turn this exception into a liveness reason as we assume
				 * that liveness checkers should be valid in themselves and should not endanger
				 * the application. The log will help to find broken liveness checkers
				 */
				logger.log(Level.ERROR, "Error while liveness checking", e);
			}
		}
		
		return switch (errors.size()) {
			case 0 -> null;
			case 1 -> errors.getFirst();
			default -> Reason.create("Multiple application fatalities", errors); 
		};
	}

	@Override
	public boolean isReady() {
		return state == RxApplicationState.started;
	}

	@Override
	public RxApplicationState getState() {
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
	
	public void setDead(Reason deathReason) {
		this.deathReason = deathReason;
		logger.log(Level.ERROR, "Application is DEAD because: " + deathReason.stringify(true));
		setState(RxApplicationState.dead);
	}

	@Override
	public void addStateChangeListener(Consumer<RxApplicationState> listener) {
		stateListeners.add(listener);
	}

	@Override
	public void addLivenessChecker(RxApplicationLivenessChecker checker) {
		livenessCheckers.add(checker);
	}
	
	private void notifyState() {
		RxApplicationState currentState = state;
		
		for (Consumer<RxApplicationState> listener: stateListeners) {
			listener.accept(currentState);
		}
	}
}
