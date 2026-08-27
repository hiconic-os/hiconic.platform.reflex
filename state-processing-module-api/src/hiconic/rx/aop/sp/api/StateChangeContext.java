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
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.PropertyManipulation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

/**
 * the interface that defines a state change context
 * 
 * contains all information required for a {@link StateChangeProcessor} to execute its payload code:<br/>
 * <br/>
 * if the context is passed in the {@link StateChangeProcessor#onBeforeStateChange} function, the data reflects the
 * state BEFORE the manipulation are applied, if passed in {@link StateChangeProcessor#onAfterStateChange} function, it
 * obviously reflects the state AFTER the application. The data influenced by the application are updated.<br/>
 * <br/>
 * <br/>
 * {@link EntityProperty} - the entity property that is involved <br/>
 * {@link PersistenceGmSession} - the session that allows the processor to commit its eventual changes<br/>
 * {@link GenericEntity} - the entity property that is involved, deriving from it that is<br/>
 * {@link PropertyManipulation} - the PropertyManipulation the is the manipulation that triggers the call. <br/>
 * 
 * @author pit
 * @author dirk
 */

public interface StateChangeContext<T extends GenericEntity> {

	/**
	 * access to the entity reference
	 * 
	 * @return - the EntityReference involved
	 */
	EntityReference getEntityReference();

	/**
	 * access to the entity property involved
	 * 
	 * @return - the EntityProperty involved
	 */
	EntityProperty getEntityProperty();

	/**
	 * access to the persistence session - use it commit any changes that might happen in your processor
	 * 
	 * @return - the PersistenceGmSession
	 */
	PersistenceGmSession getSession();

	PersistenceGmSession getSystemSession();

	/**
	 * returns the {@link CmdResolver} initialized with {@link GmMetaModel} of the access that is behind
	 * the {@link PersistenceGmSession} accessible via {@link #getSession()}.
	 */
	CmdResolver getCmdResolver();

	boolean wasSessionModified();
	
	boolean wasSystemSessionModified();

	/**
	 * access to the process entity we're attached to
	 * 
	 * @return - the process entity (an extension of GenericEntity)
	 */
	T getProcessEntity() throws StateChangeProcessorException;


	T getSystemProcessEntity() throws StateChangeProcessorException;

	/**
	 * access to the actual manipulation that triggered the call if it's a ChangeValueManipulation, it gives access to
	 * the intended/new-value of the property
	 * 
	 * @return - the manipulation involved (an extension of PropertyManipulation)
	 */
	<M extends Manipulation> M getManipulation();

	/**
	 * @return - the entity type involved
	 */
	<T1 extends T> EntityType<T1> getEntityType();

	/**
	 * Returns the context which was acquired by {@link StateChangeProcessor#createProcessorContext()}.
	 */
	<E> E getProcessorContext();
	
	void overrideCapabilities(StateChangeProcessorCapabilities capabilities);
}
