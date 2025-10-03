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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

import hiconic.rx.check.model.bundle.api.response.CbrAggregatable;
import hiconic.rx.check.model.bundle.api.response.CbrAggregation;
import hiconic.rx.check.model.bundle.api.response.CbrAggregationKind;
import hiconic.rx.check.model.bundle.api.response.CheckBundleResult;
import hiconic.rx.check.model.bundle.aspect.CheckCoverage;
import hiconic.rx.check.model.bundle.aspect.CheckLatency;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;

/**
 * Utility functions for check bundle processing.
 * 
 * @author christina.wilpernig
 */
public class CheckBundlesUtils {

	public static CheckStatus getStatus(Collection<CheckResult> results) {
		return results.stream() //
				.flatMap(c -> c.getEntries().stream()) //
				.map(CheckResultEntry::getCheckStatus) //
				.max(Comparator.naturalOrder()) //
				.orElse(CheckStatus.ok);
	}
	
	public static CheckStatus getStatus(CheckResult result) {
		return result.getEntries().stream() //
				.map(CheckResultEntry::getCheckStatus) //
				.max(Comparator.naturalOrder()) //
				.orElse(CheckStatus.ok);
	}

	public static Function<CheckBundleResult, Collection<?>> getAccessor(CbrAggregationKind kind) {
		// @formatter:off
		switch (kind) {
			case coverage: 			return r -> Collections.singleton(r.getCoverage());
			case label: 			return r -> r.getLabels();
			case node: 				return r -> Collections.singleton(r.getNode());
			case processorName: 	return r -> Collections.singleton(r.getCheckProcessorName());
			case status: 			return r -> Collections.singleton(r.getStatus());
			case latency: 			return r -> Collections.singleton(r.getLatency());
			case bundle: 			return r -> Collections.singleton(r.getName());
			default:
				throw new IllegalStateException("Unknown CbrAggregationKind: " + kind);
		}
		// @formatter:on
	}

	public static String getIdentification(CbrAggregatable a) {
		if (a.isResult())
			return ((CheckBundleResult) a).getName();

		CbrAggregation aggr = (CbrAggregation) a;
		CbrAggregationKind kind = aggr.getKind();
		Object d = aggr.getDiscriminator();

		switch (kind) {
			case bundle:
				return (String) d;
			case coverage:
				return ((CheckCoverage) d).name();
			case label:
				return (String) d;
			case node:
				return (String) d;
			case processorName:
				return (String) d;
			case status:
				return ((CheckStatus) d).name();
			case latency:
				return ((CheckLatency) d).name();
			default:
				throw new IllegalStateException("Unknown CheckAggregationKind: " + kind);
		}
	}
}
