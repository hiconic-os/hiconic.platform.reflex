//============================================================================
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//============================================================================
package hiconic.rx.check.processing;

import static com.braintribe.utils.lcd.CollectionTools2.asLinkedSet;
import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.braintribe.logging.Logger;
import com.braintribe.utils.lcd.CollectionTools2;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.api.CheckProcessorRegistry;
import hiconic.rx.check.api.CheckProcessorSymbol;
import hiconic.rx.check.model.bundle.api.request.RunCheckBundles;
import hiconic.rx.check.model.bundle.aspect.CheckCoverage;
import hiconic.rx.check.model.bundle.aspect.CheckLatency;

/**
 * @author peter.gazdik
 */
public class BasicCheckProcessorRegistry implements CheckProcessorRegistry {

	private static final Logger log = Logger.getLogger(BasicCheckProcessorRegistry.class);

	private final Map<String, CheckProcessorEntry> checkProcessorEntries = newConcurrentMap();
	private final Map<String, CheckBundle> checkBundles = newConcurrentMap();

	interface CheckBundle {
		List<CheckProcessorEntry> listCheckProcessors();
	}

	public static record CheckProcessorEntry( //
			String name, //
			CheckProcessor processor, //
			CheckCoverage coverage, //
			CheckLatency latency, //
			Set<String> labels //
	) implements CheckBundle {
		@Override
		public List<CheckProcessorEntry> listCheckProcessors() {
			return Collections.singletonList(this);
		}
	}

	@Override
	public void registerProcessor(CheckProcessorSymbol name, CheckProcessor processor, CheckCoverage coverage, CheckLatency latency,
			String... labels) {
		NullSafe.nonNull(name, "name");
		NullSafe.nonNull(processor, "processor");
		NullSafe.nonNull(coverage, "coverage");
		NullSafe.nonNull(latency, "latency");

		String _name = name.name();

		CheckProcessorEntry entry = new CheckProcessorEntry(_name, processor, coverage, latency, asLinkedSet(labels));

		checkProcessorEntries.put(_name, entry);
		registerInternal(name, entry);
	}

	@Override
	public void registerBundleProcessor(CheckProcessorSymbol name, CheckProcessorSymbol... processorNames) {
		checkBundleInput(name, (Object[]) processorNames);

		registerBundle(name, Stream.of(processorNames).map(CheckProcessorSymbol::name).toList());
	}

	@Override
	public void registerBundleProcessor(CheckProcessorSymbol name, String... processorNames) {
		checkBundleInput(name, (Object[]) processorNames);
		registerBundle(name, Arrays.asList(processorNames));
	}

	private void checkBundleInput(CheckProcessorSymbol name, Object... processorNames) {
		NullSafe.nonNull(name, "name");
		NullSafe.nonNull(processorNames, "processorNames");
		if (processorNames.length == 0)
			throw new IllegalArgumentException("Cannot register a bundle processor with no processors.");
	}

	private void registerBundle(CheckProcessorSymbol name, List<String> processorNames) {
		List<CheckBundle> processors = requireProcessors(processorNames);
		registerInternal(name, new ListBundleCheckProcessor(processors));
	}

	private List<CheckBundle> requireProcessors(List<String> processorNames) {
		return processorNames.stream().map(this::requireProcessor).toList();
	}

	private CheckBundle requireProcessor(String processorName) {
		CheckBundle processor = checkBundles.get(processorName);
		if (processor == null)
			throw new IllegalArgumentException("Unknown check processor or bundle: " + processorName);
		return processor;
	}

	@Override
	public void registerBundleProcessor(CheckProcessorSymbol name, CheckCoverage coverage, CheckLatency latency) {
		registerInternal(name, new CategoryFilteringBundleCheckProcessor(coverage, latency));
	}

	private void registerInternal(CheckProcessorSymbol name, CheckBundle processor) {
		checkBundles.put(name.name(), processor);
	}

	// ##############################################################
	// ## . . . . . . . . ListBundleCheckProcessor . . . . . . . . ##
	// ##############################################################

	static class ListBundleCheckProcessor implements CheckBundle {

		private final List<CheckBundle> processors;

		public ListBundleCheckProcessor(List<CheckBundle> processors) {
			this.processors = processors;
		}

		@Override
		public List<CheckProcessorEntry> listCheckProcessors() {
			return processors.stream() //
					.flatMap(checker -> checker.listCheckProcessors().stream()) //
					.toList();
		}
	}

	// ##############################################################
	// ## . . . . . . . . ListBundleCheckProcessor . . . . . . . . ##
	// ##############################################################

	class CategoryFilteringBundleCheckProcessor implements CheckBundle {

		private final CheckCoverage coverage;
		private final CheckLatency latency;

		public CategoryFilteringBundleCheckProcessor(CheckCoverage coverage, CheckLatency latency) {
			this.coverage = coverage;
			this.latency = latency;
		}

		@Override
		public List<CheckProcessorEntry> listCheckProcessors() {
			return checkProcessorEntries.values().stream() //
					.filter(this::matchesFilter) //
					.toList();
		}

		private boolean matchesFilter(CheckProcessorEntry entry) {
			return (coverage == null || coverage == entry.coverage()) && //
					(latency == null || latency == entry.latency());
		}
	}

	// ##############################################################
	// ## . . . . . . . . ListBundleCheckProcessor . . . . . . . . ##
	// ##############################################################

	public List<CheckProcessorEntry> listMatchingProcessorEntries(RunCheckBundles request) {
		return streamMatchingBundles(request) //
				.flatMap(b -> b.listCheckProcessors().stream()) //
				.filter(matchingEntryPredicate(request)) //
				.toList();
	}

	private Stream<CheckBundle> streamMatchingBundles(RunCheckBundles request) {
		Set<String> names = request.getName();
		if (names.isEmpty())
			return checkBundles.values().stream();
		else
			return names.stream() //
					.map(this::findBundle) //
					.filter(b -> b != null);
	}

	private static Predicate<CheckProcessorEntry> matchingEntryPredicate(RunCheckBundles request) {
		Predicate<CheckProcessorEntry> result = e -> true;

		// # Coverage
		Set<CheckCoverage> coverages = request.getCoverage();
		if (!coverages.isEmpty())
			result = result.and(//
					e -> coverages.contains(e.coverage()));

		// # Latency
		CheckLatency latency = request.getLatency();
		if (latency != null)
			result = result.and( //
					e -> e.latency().ordinal() <= latency.ordinal());

		// # Labels
		Set<String> labels = request.getLabel();
		if (!labels.isEmpty())
			result = result.and( //
					e -> CollectionTools2.containsAny(e.labels(), labels));

		return result;
	}

	private CheckBundle findBundle(String bundleName) {
		CheckBundle result = checkBundles.get(bundleName);
		if (result == null)
			log.warn("Won't run check for bundle [" + bundleName + "], no such bundle found.");

		return result;
	}
}
