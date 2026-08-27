package hiconic.rx.aop.sp.registry;

import static com.braintribe.utils.lcd.CollectionTools2.newMap;

import java.util.Map;
import java.util.Objects;

import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateProcessorRegistry;

/**
 * @author peter.gazdik
 */
public class StateProcessorRegistryImpl implements StateProcessorRegistry {

	private final Map<String, StateChangeProcessor<?, ?>> processorMap = newMap();

	@Override
	public StateChangeProcessor<?, ?> getStateChangeProcessor(String processorId) {
		StateChangeProcessor<?, ?> result = processorMap.get(processorId);
		if (result == null)
			throw new IllegalArgumentException("No StateChangeProcessor registered for processorId: " + processorId);

		return result;
	}

	public void registerStateChangeProcessor(StateChangeProcessor<?, ?> stateChangeProcessor) {
		Objects.requireNonNull(stateChangeProcessor, "stateChangeProcessor");

		String processorId = stateChangeProcessor.processorId();
		Objects.requireNonNull(processorId, "processorId");

		if (processorMap.containsKey(processorId))
			throw new IllegalArgumentException("StateChangeProcessor already registered for processorId: " + processorId);

		processorMap.put(processorId, stateChangeProcessor);
	}

}
