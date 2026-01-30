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
package hiconic.rx.access.api;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.accessapi.AccessRequest;

/**
 * TODO 
 */
@FunctionalInterface
public interface ReasonedAccessRequestProcessor<P extends AccessRequest, R> extends AccessRequestProcessor<P, R> {
	@Override
	default R process(AccessRequestContext<P> context) {
		return getOrTunnel(processReasoned(context));
	}
	
	Maybe<? extends R> processReasoned(AccessRequestContext<P> context);
}
