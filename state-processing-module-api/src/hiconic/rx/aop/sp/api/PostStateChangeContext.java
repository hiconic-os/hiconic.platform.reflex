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

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.generic.value.PreliminaryEntityReference;

/**
 * An interface for methods that are common for {@link AfterStateChangeContext} and {@link ProcessStateChangeContext}.
 */
public interface PostStateChangeContext<T extends GenericEntity> extends StateChangeContext<T> {

	/**
	 * Returns a mapping from {@link EntityReference} to {@link PersistentEntityReference}.
	 * 
	 * Note that in some cases not all the persistent references are contained as keys, but all the
	 * {@link PreliminaryEntityReference}s must be resolvable via the returned mapping.
	 */
	Map<EntityReference, PersistentEntityReference> getReferenceMap();

	void notifyInducedManipulation(Manipulation manipulation);

}
