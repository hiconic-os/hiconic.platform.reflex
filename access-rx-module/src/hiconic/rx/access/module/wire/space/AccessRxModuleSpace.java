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
package hiconic.rx.access.module.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.gm.model.persistence.reflection.api.PersistenceReflectionRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.PlatformRequest;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.model.configuration.AccessConfiguration;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.access.module.processing.PersistenceReflectionProcessor;
import hiconic.rx.access.module.processing.RxAccessModelConfigurations;
import hiconic.rx.access.module.processing.RxAccesses;
import hiconic.rx.access.module.processing.RxPersistenceGmSessionFactory;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class AccessRxModuleSpace implements RxModuleContract, AccessContract, AccessExpertContract {

	@Import
	private RxPlatformContract platform;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		AccessConfiguration accessConfiguration = getOrTunnel(platform.readConfig(AccessConfiguration.T));
		
		for (Access access: accessConfiguration.getAccesses()) {
			deploy(access);
		}
	}
	
	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configurations.byId(PlatformRequest.platformDomainId).bindRequest(PersistenceReflectionRequest.T, this::persistenceReflectionProcessor);
	}
	
	@Override
	public void configureModels(ModelConfigurations configurations) {
		accesses().initModelConfigurations(configurations);
		accessModelConfigurations().initModelConfigurations(configurations);
	}
	
	@Override
	@Managed
	public RxAccessModelConfigurations accessModelConfigurations() {
		return new RxAccessModelConfigurations();
	}
	
	@Override
	public <A extends Access> void registerAccessExpert(EntityType<A> accessType, AccessExpert<A> expert) {
		accesses().registerExpert(accessType, expert);
	}
	
	@Override
	public void deploy(Access access) {
		accesses().deploy(access);
	}
	
	@Override
	public AccessDomains accessDomains() {
		return accesses();
	}

	@Override
	@Managed
	public RxPersistenceGmSessionFactory contextSessionFactory() {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(AttributeContexts::peek);
		configure(bean);
		return bean;
	}
	
	@Override
	public RxPersistenceGmSessionFactory sessionFactory(AttributeContext attributeContext) {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(() -> attributeContext);
		configure(bean);
		return bean;
	}
	
	@Override
	@Managed
	public RxPersistenceGmSessionFactory systemSessionFactory() {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(platform.systemAttributeContextSupplier());
		configure(bean);
		return bean;
	}
	
	@Managed 
	public PersistenceReflectionProcessor persistenceReflectionProcessor() {
		PersistenceReflectionProcessor bean = new PersistenceReflectionProcessor();
		bean.setAccesses(accesses());
		return bean;
	}
	
	private void configure(RxPersistenceGmSessionFactory bean) {
		bean.setAccesses(accesses());
		bean.setEvaluatorSupplier(platform::evaluator);
	}
	
	@Managed
	private RxAccesses accesses() {
		RxAccesses bean = new RxAccesses();
		bean.setConfiguredModels(platform.configuredModels());
		bean.setServiceDomains(platform.serviceDomains());
		bean.setContextSessionFactory(contextSessionFactory());
		bean.setSystemSessionFactory(systemSessionFactory());
		return bean;
	}
}