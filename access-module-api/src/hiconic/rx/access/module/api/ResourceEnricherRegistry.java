package hiconic.rx.access.module.api;

/**
 * @author peter.gazdik
 */
public interface ResourceEnricherRegistry {

	/** Registers a {@link ResourceEnricher} with a given name. */
	void registerEnricher(ResourceEnricherSymbol enricherName, ResourceEnricher enricher);

	/** Returns {@link ResourceEnricher} with given name or throws an {@link IllegalStateException}. */
	ResourceEnricher getEnricher(ResourceEnricherSymbol enricherName);
	/** Returns {@link ResourceEnricher} with given name or throws an {@link IllegalStateException}. */
	ResourceEnricher getEnricher(String name);

	/** Returns {@link ResourceEnricher} with given name or null. */
	ResourceEnricher findEnricher(ResourceEnricherSymbol enricherName);
	/** Returns {@link ResourceEnricher} with given name or null. */
	ResourceEnricher findEnricher(String name);

}
