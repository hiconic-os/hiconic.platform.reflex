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
package hiconic.rx.check.model.bundle.aspect;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Classifies checks based on the expected computational effort.
 * 
 * @see CheckCoverage
 */
public enum CheckLatency implements EnumBase<CheckLatency> {

	/** Checks that are very fast and use little resources. These checks can be performed frequently. */
	immediate,

	/** Checks that are still reasonably fast, up to say a few seconds, but calling them excessively could affect performance. */
	moderate,

	/** Deep(er) checks which should ideally be performed at off-peak hours. */
	extensive;

	public static final EnumType<CheckLatency> T = EnumTypes.T(CheckLatency.class);

	@Override
	public EnumType<CheckLatency> type() {
		return T;
	}
}
