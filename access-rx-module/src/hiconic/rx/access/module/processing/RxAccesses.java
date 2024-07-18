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
package hiconic.rx.access.module.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.cfg.Required;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.access.impl.InternalizingPersistenceProcessor;
import com.braintribe.model.access.impl.aop.AopAccess;
import com.braintribe.model.accessapi.PersistenceRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.core.expert.api.MutableDenotationMap;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.model.md.InterceptAccessWith;
import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.service.ServiceDomains;

public class RxAccesses {
	private Map<String, LazyInitialized<RxAccess>> accesses = new ConcurrentHashMap<>();
	private MutableDenotationMap<Access, AccessExpert<?>> experts = new PolymorphicDenotationMap<Access, AccessExpert<?>>(true);
	private ConfiguredModels configuredModels;
	private ModelConfigurations modelConfigurations;
	private ServiceDomainConfigurations serviceDomainConfigurations;
	private PersistenceGmSessionFactory systemSessionFactory;
	private PersistenceGmSessionFactory contextSessionFactory;
	private ServiceDomains serviceDomains;
	
	@Required
	public void setServiceDomainConfigurations(ServiceDomainConfigurations serviceDomainConfigurations) {
		this.serviceDomainConfigurations = serviceDomainConfigurations;
	}
	
	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}
	
	@Required
	public void setConfiguredModels(ConfiguredModels configuredModels) {
		this.configuredModels = configuredModels;
	}
	
	@Required
	public void setSystemSessionFactory(PersistenceGmSessionFactory systemSessionFactory) {
		this.systemSessionFactory = systemSessionFactory;
	}
	
	@Required
	public void setContextSessionFactory(PersistenceGmSessionFactory contextSessionFactory) {
		this.contextSessionFactory = contextSessionFactory;
	}

	public void initModelConfigurations(ModelConfigurations modelConfigurations) {
		this.modelConfigurations = modelConfigurations;
	}
	
	public <A extends Access> void registerExpert(EntityType<A> accessType, AccessExpert<A> expert) {
		experts.put(accessType, expert);
	}
	
	public void deploy(Access access) {
		Objects.requireNonNull(access.getDataModelName(), "Access.dataModelName must not be null");
		String accessId = Objects.requireNonNull(access.getAccessId(), "Access.accessId must not be null");
		String serviceModelName = access.getServiceModelName();
		String serviceDomainId = access.getServiceDomainId();
		
		if (serviceModelName != null || serviceDomainId != null) {
			ModelConfiguration serviceModelConfiguration = null;
			
			if (serviceModelName != null) {
				serviceModelConfiguration = modelConfigurations.byName(serviceModelName);
			}
			
			if (serviceDomainId != null) {
				ModelConfiguration serviceDomainConfiguration = serviceDomainConfigurations.byId(serviceDomainId);
				
				if (serviceModelConfiguration != null)
					serviceDomainConfiguration.addModel(serviceModelConfiguration);
				else
					serviceModelConfiguration = serviceDomainConfiguration;
			}
			
			serviceModelConfiguration.bindRequest(PersistenceRequest.T, () -> getPersistenceProcessor(accessId));
		}
		
		LazyInitialized<RxAccess> lazyAccess = new LazyInitialized<>(() -> deployNow(access));
		if (accesses.putIfAbsent(accessId, lazyAccess) != null)
			throw new IllegalArgumentException("Duplicate deployment of an Access with id: " + accessId);
	}
	
	public InternalizingPersistenceProcessor getPersistenceProcessor(String accessId) {
		return new InternalizingPersistenceProcessor(getAccess(accessId).incrementalAccess());
	}

	public RxAccess getAccess(String accessId) {
		LazyInitialized<RxAccess> lazyAccess = accesses.get(accessId);
		
		if (lazyAccess == null)
			throw new NoSuchElementException("No Access configured with accessId: " + accessId);
		
		return lazyAccess.get();
	}
	
	private RxAccess deployNow(Access access) {
		AccessExpert<Access> accessExpert = experts.get(access);
		String dataModelName = access.getDataModelName();
		
		ConfiguredModel dataModel = configuredModels.byName(dataModelName);
		String serviceDomainId = access.getServiceDomainId();
		
		ConfiguredModel serviceModel = serviceDomainId != null? 
				serviceDomains.byId(serviceDomainId).configuredModel():
				null;
		
		// TODO: work with reasons here
		Objects.requireNonNull(dataModel, "Access.dataModelName = '" + dataModelName + "' not found for access: " + access.getAccessId());
		IncrementalAccess incrementalAccess = accessExpert.deploy(access, dataModel).get();
		
		CmdResolver systemCmdResolver = dataModel.systemCmdResolver();
		
		// wrap with AopAccess if interceptors are present
		List<InterceptAccessWith> interceptions = systemCmdResolver.getMetaData().meta(InterceptAccessWith.T).list();
		
		if (!interceptions.isEmpty()) {
			List<AccessAspect> aspects = new ArrayList<>();
			
			for (InterceptAccessWith interceptWith: interceptions) {
				aspects.add(interceptWith.getAssociate());
			}
			
			AopAccess aopAccess = new AopAccess();
			aopAccess.setAccessId(access.getAccessId());
			aopAccess.setDelegate(incrementalAccess);
			aopAccess.setSystemSessionFactory(systemSessionFactory);
			aopAccess.setUserSessionFactory(contextSessionFactory);
			aopAccess.setAspects(aspects);
			
			incrementalAccess = aopAccess;
		}
		
		return new RxAccess(access, incrementalAccess, dataModel, serviceModel);
	}

	
}
