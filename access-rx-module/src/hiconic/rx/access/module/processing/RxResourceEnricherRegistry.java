package hiconic.rx.access.module.processing;

import java.util.HashMap;
import java.util.Map;

import hiconic.rx.access.module.api.ResourceEnricher;
import hiconic.rx.access.module.api.ResourceEnricherRegistry;
import hiconic.rx.access.module.api.ResourceEnricherSymbol;

/**
 * @author peter.gazdik
 */
public class RxResourceEnricherRegistry implements ResourceEnricherRegistry {

	private final Map<String, ResourceEnricher> enrichers = new HashMap<>();

	@Override
	public void registerEnricher(ResourceEnricherSymbol enricherName, ResourceEnricher enricher) {
		enrichers.put(enricherName.name(), enricher);
	}

	@Override
	public ResourceEnricher getEnricher(ResourceEnricherSymbol enricherName) {
		return getEnricher(enricherName.name());
	}

	@Override
	public ResourceEnricher getEnricher(String name) {
		ResourceEnricher enricher = findEnricher(name);
		if (enricher == null)
			throw new IllegalStateException("Enricher not found: " + name);

		return enricher;
	}

	@Override
	public ResourceEnricher findEnricher(ResourceEnricherSymbol enricherName) {
		return findEnricher(enricherName.name());
	}

	@Override
	public ResourceEnricher findEnricher(String name) {
		return enrichers.get(name);
	}

}
