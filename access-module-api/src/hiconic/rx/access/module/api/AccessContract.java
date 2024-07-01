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
package hiconic.rx.access.module.api;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.wire.RxExportContract;

public interface AccessContract extends RxExportContract {

	void deploy(Access access);
	
	AccessModelConfigurations accessModelConfigurations();

	PersistenceGmSessionFactory contextSessionFactory();
	
	PersistenceGmSessionFactory systemSessionFactory();
	
	PersistenceGmSessionFactory sessionFactory(AttributeContext attributeContext);
	
}

/*
 * access-configuration-model (defines base denotation type Access)
 * access-module-api
 * access-rx-module (implements: AccessContract & AccessExpertContract)
 *   - expert registration and usage
 *   - reading AccessConfiguration and automatically deploying it
 *   - Access to deployed Accesses via session factories
 *   
 * hibernate-access-configuration-model
 * hibernate-access-rx-module
 * 
 * smood-access-configuration-model
 * smood-access-rx-module
 * 
 */
