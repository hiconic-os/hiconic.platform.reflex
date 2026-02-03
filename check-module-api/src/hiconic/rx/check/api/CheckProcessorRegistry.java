package hiconic.rx.check.api;

import hiconic.rx.check.model.aspect.CheckCoverage;
import hiconic.rx.check.model.aspect.CheckLatency;

/**
 * @author peter.gazdik
 */
public interface CheckProcessorRegistry {

	/**
	 * Registers a {@link CheckProcessor} with a given name.
	 * 
	 * @see CheckCoverage
	 * @see CheckLatency
	 */
	void registerProcessor(CheckProcessorSymbol name, CheckProcessor processor, CheckCoverage coverage, CheckLatency latency, String... labels);

	/**
	 * Registers a "bundle" {@link CheckProcessor} consisting of processors whose names are passed as arguments.
	 */
	void registerBundleProcessor(CheckProcessorSymbol name, CheckProcessorSymbol... processorNames);

	/** @see #registerBundleProcessor(CheckProcessorSymbol, CheckProcessorSymbol...) */
	void registerBundleProcessor(CheckProcessorSymbol name, String... processorNames);

	/**
	 * Registers a "bundle" {@link CheckProcessor} consisting of processors matches given {@link CheckCoverage coverage} and {@link CheckLatency
	 * latency}.
	 * <p>
	 * Passing <tt>null</tt> for either value is interpreted as a wildcard matching all values.
	 */
	void registerBundleProcessor(CheckProcessorSymbol name, CheckCoverage coverage, CheckLatency latency);

}
