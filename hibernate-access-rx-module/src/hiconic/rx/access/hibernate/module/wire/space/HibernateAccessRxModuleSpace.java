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
package hiconic.rx.access.hibernate.module.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.hibernate.model.configuration.HibernateAccess;
import hiconic.rx.access.hibernate.processing.HibernateAccessExpert;
import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class HibernateAccessRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private AccessExpertContract accessExpert;
	
	@Import
	private HibernateContract hibernate;
	
	@Import
	private DatabaseContract database;
	
	@Override
	public void onDeploy() {
		accessExpert.registerAccessExpert(HibernateAccess.T, hibernateAccessExpert());
	}
	
	@Managed
	private HibernateAccessExpert hibernateAccessExpert() {
		HibernateAccessExpert bean = new HibernateAccessExpert();
		bean.setDatabaseContract(database);
		bean.setHibernateContract(hibernate);
		return bean;
	}


}