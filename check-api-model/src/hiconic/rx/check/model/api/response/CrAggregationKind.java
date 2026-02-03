// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.check.model.api.response;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum CrAggregationKind implements EnumBase<CrAggregationKind> {

	node,
	label,
	processorName,
	latency,
	coverage,
	status,
	bundle;

	public static final EnumType<CrAggregationKind> T = EnumTypes.T(CrAggregationKind.class);

	@Override
	public EnumType<CrAggregationKind> type() {
		return T;
	}
}
