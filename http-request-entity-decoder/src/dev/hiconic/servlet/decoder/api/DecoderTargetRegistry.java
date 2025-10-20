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
package dev.hiconic.servlet.decoder.api;

import com.braintribe.model.generic.GenericEntity;

public interface DecoderTargetRegistry {

	/**
	 * Adds a new target entity to receive values from the decoded request.
	 * 
	 * @param prefix
	 *            the prefix to use to bypass ordering, must not be {@code null}
	 * @param target
	 *            the target entity to decode into, must not be {@code null}
	 * @param onSet
	 * 			  code that gets executed when the target is actually addressed and set in a request
	 * 
	 * @return this decoder
	 */
	DecoderTargetRegistry target(String prefix, GenericEntity target, Runnable onSet);

}