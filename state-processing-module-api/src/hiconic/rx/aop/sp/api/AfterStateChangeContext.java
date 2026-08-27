// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.aop.sp.api;

import com.braintribe.model.generic.GenericEntity;

/**
 * the context for the call {@link StateChangeProcessor}.onAfterStateChange
 * 
 * @param <T>
 *            : the process entity involved
 * 
 * @author pit
 * @author dirk
 */
public interface AfterStateChangeContext<T extends GenericEntity> extends PostStateChangeContext<T> {
	// empty
}
