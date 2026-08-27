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

import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

/**
 * the context that is passed to the {@link StateChangeProcessorRule} contains all relevant data :<br/>
 * 
 * {@link Manipulation} - the property manipulation involved<br/>
 * {@link EntityProperty} - the owning entity property if present<br/>
 * {@link EntityType} - the entity type of the entity involved <br/>
 * {@link Property} - the property involved if present<br/>
 * 
 * @author pit
 * @author dirk
 */
public interface StateChangeProcessorSelectorContext {

	/** @return - the property manipulation */
	<M extends Manipulation> M getManipulation();

	/** @return - the entity reference which the manipulation targets */
	EntityReference getEntityReference();

	/** @return - the entity property involved if a property related manipulation is given */
	EntityProperty getEntityProperty();

	/** @return - the entity type involved */
	EntityType<?> getEntityType();

	/** @return - the property involved */
	Property getProperty();

	CmdResolver getCmdResolver();

	PersistenceGmSession getSession();
	PersistenceGmSession getSystemSession();

	<T> T getCustomContext();
	void setCustomContext(Object customContext);

	boolean isForLifecycle();
	boolean isForInstantiation();
	boolean isForDeletion();
	boolean isForProperty();
}
