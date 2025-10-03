package hiconic.rx.platform.processing.worker;

import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentMap;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.braintribe.logging.Logger;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.WorkerAspect;
import com.braintribe.model.processing.worker.api.WorkerAspectContext;
import com.braintribe.utils.collection.PartiallyOrderedSet;
import com.braintribe.utils.collection.PartiallyOrderedSet.PosAdditionContext;
import com.braintribe.utils.collection.impl.SimplePartiallyOrderedSet;
import com.braintribe.utils.lcd.NullSafe;

/**
 * @author peter.gazdik
 */
public class BasicWorkerAspectRegistry implements ConfigurableWorkerAspectRegistry {

	private final Map<String, WorkerAspect> idToAspect = newConcurrentMap();
	private final Map<WorkerAspect, String> aspectToId = newConcurrentMap();

	private volatile PartiallyOrderedSet<WorkerAspect, String> aspects;

	private volatile Map<String, WorkerAspect> orderedAspectMap;
	private volatile List<WorkerAspect> orderedAspectList;

	private static final Logger log = Logger.getLogger(WorkerAspectCallable.class);

	@Override
	public Map<String, WorkerAspect> aspects() {
		if (orderedAspectMap == null)
			synchronized (this) {
				if (orderedAspectMap == null)
					orderedAspectMap = aspects.stream().collect(Collectors.toUnmodifiableMap(//
							e -> aspectToId.get(e), //
							e -> e//
					));
			}

		return orderedAspectMap;
	}

	
	/** Wraps the given {@link Callable} with all the registered {@link WorkerAspect}s in the order of execution. */
	public <T> Callable<T> wrap(Callable<T> callable) {
		return new WorkerAspectCallable<T>(listAspects(), callable);
	}

	@Override
	public void register(String identifier, WorkerAspect aspect) {
		NullSafe.nonNull(identifier, "identifier");
		NullSafe.nonNull(aspect, "aspect");

		idToAspect.put(identifier, aspect);
		aspectToId.put(aspect, identifier);
	}

	@Override
	public synchronized void order(String... identifiers) {
		orderedAspectMap = null;
		orderedAspectList = null;

		aspects = new SimplePartiallyOrderedSet<>();

		HashMap<String, WorkerAspect> _idToAspect = newMap(idToAspect);

		// Add aspects with ordering information
		String prevIdentifier = null;
		for (String identifier : identifiers) {
			WorkerAspect aspect = _idToAspect.remove(identifier);

			if (aspect == null) {
				log.warn("Will ignore unknown aspect identifier for ordering: " + identifier);
				continue;
			}

			PosAdditionContext<WorkerAspect, String> addition = aspects.with(identifier);
			if (prevIdentifier != null)
				addition = addition.after(prevIdentifier);
			else
				prevIdentifier = identifier;
			addition.add(aspect);
		}

		// Add aspects with no ordering information
		for (Entry<String, WorkerAspect> e : _idToAspect.entrySet())
			aspects.with(e.getKey()).add(e.getValue());
	}

	private List<WorkerAspect> listAspects() {
		if (orderedAspectList == null)
			synchronized (this) {
				if (orderedAspectList == null)
					orderedAspectList = aspects.stream().collect(Collectors.toUnmodifiableList());
			}

		return orderedAspectList;
	}

}

class WorkerAspectCallable<T> implements Callable<T> {

	private final List<WorkerAspect> aspects;
	private final Callable<?> delegate;

	public WorkerAspectCallable(List<WorkerAspect> aspects, Callable<?> delegate) {
		this.aspects = aspects;
		this.delegate = delegate;
	}

	@Override
	public T call() throws Exception {
		WaContext context = new WaContext(0);
		return (T) context.proceed();
	}

	private class WaContext implements WorkerAspectContext {

		private final int index;

		public WaContext(int index) {
			this.index = index;
		}

		@Override
		public Object proceed() throws Exception {
			if (index >= aspects.size())
				return delegate.call();

			WorkerAspect aspect = aspects.get(index);

			return aspect.run(new WaContext(index + 1));
		}

	}

}