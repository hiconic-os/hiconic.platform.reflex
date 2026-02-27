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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.access.impl.InternalizingPersistenceProcessor;
import com.braintribe.model.access.impl.aop.AopAccess;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.core.expert.api.MutableDenotationMap;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.utils.lcd.Lazy;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.model.md.InterceptAccessWith;
import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.access.module.api.AccessModelConfigurations;
import hiconic.rx.access.module.api.AccessModelSymbols;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.service.ServiceDomains;

public class RxAccesses implements AccessDomains {

	private final Map<String, Lazy<RxAccess>> accesses = new ConcurrentHashMap<>();
	private final MutableDenotationMap<Access, AccessExpert<?>> experts = new PolymorphicDenotationMap<>(true);

	private ConfiguredModels configuredModels;
	private AccessModelConfigurations accessModelConfigurations;
	private ServiceDomainConfigurations serviceDomainConfigurations;
	private PersistenceGmSessionFactory systemSessionFactory;
	private PersistenceGmSessionFactory contextSessionFactory;
	private ServiceDomains serviceDomains;

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

	@Required
	public void setAccessModelConfigurations(AccessModelConfigurations accessModelConfigurations) {
		this.accessModelConfigurations = accessModelConfigurations;
	}

	public void initServiceDomainConfigurations(ServiceDomainConfigurations serviceDomainConfigurations) {
		this.serviceDomainConfigurations = serviceDomainConfigurations;
	}

	public <A extends Access> void registerExpert(EntityType<A> accessType, AccessExpert<A> expert) {
		experts.put(accessType, expert);
	}

	@Override
	public Set<String> domainIds() {
		return Set.copyOf(accesses.keySet());
	}

	@Override
	public boolean hasDomain(String domainId) {
		return accesses.containsKey(domainId);
	}

	@Override
	public AccessDomain byId(String domainId) {
		return findAccess(domainId);
	}

	private RxAccess findAccess(String domainId) {
		Lazy<RxAccess> lazyAccess = accesses.get(domainId);
		return lazyAccess == null ? null : lazyAccess.get();
	}

	// Unused?
	public InternalizingPersistenceProcessor getPersistenceProcessor(String accessId) {
		return new InternalizingPersistenceProcessor(getAccess(accessId).incrementalAccess());
	}

	public RxAccess getAccess(String accessId) {
		Lazy<RxAccess> lazyAccess = accesses.get(accessId);
		if (lazyAccess == null)
			throw new NoSuchElementException("No Access configured with accessId: " + accessId);

		return lazyAccess.get();
	}

	public void deploy(Access access) {
		deploy(access, () -> resolveAndRegister(access));
	}

	public void deploy(Access accessDenotation, IncrementalAccess access) {
		deploy(accessDenotation, () -> register(accessDenotation, access));
	}
	
	/* TODO: extract the service-domain/model configuration into the right phase and only use it here */
	private void deploy(Access access, Supplier<RxAccess> rxAccessSupplier) {
		String accessId = access.getAccessId();

		NullSafe.nonNull(accessId, "Access.accessId");
		
		for (String dataModelName: access.getDataModelNames())
			accessModelConfigurations.dataModelConfiguration(accessId).addModelByName(dataModelName);

		// This creates a new ServiceDomain in the system
		ServiceDomainConfiguration sdConfiguration = serviceDomainConfigurations.byId(accessId);

		sdConfiguration.addModel(AccessModelSymbols.configuredAccessApiModel);

		for (String serviceModelName: access.getServiceModelNames())
			sdConfiguration.addModelByName(serviceModelName);

		if (accesses.putIfAbsent(accessId, new Lazy<>(rxAccessSupplier)) != null)
			throw new IllegalArgumentException("Duplicate deployment of an Access with id: " + accessId);
	}

	/**
	 * This:
	 * <ol>
	 * <li>Resolves an {@link IncrementalAccess} based on an expert configured for given access denotation
	 * <li>Registers this incremental access via {@link #register(Access, IncrementalAccess)}
	 * </ol>
	 */
	private RxAccess resolveAndRegister(Access access) {
		AccessExpert<Access> accessExpert = experts.get(access);

		ConfiguredModel dataModel = getDataModelOf(access);

		IncrementalAccess incrementalAccess = accessExpert.deploy(access, dataModel).get();

		return register(access, incrementalAccess);
	}

	private RxAccess register(Access access, IncrementalAccess incrementalAccess) {
		ConfiguredModel dataModel = getDataModelOf(access);
		ConfiguredModel serviceModel = getServiceModelOf(access);

		// wrap with AopAccess if interceptors are present
		List<InterceptAccessWith> interceptions = dataModel.systemCmdResolver() //
				.getMetaData() //
				.meta(InterceptAccessWith.T) //
				.list();

		if (!interceptions.isEmpty()) {
			List<AccessAspect> aspects = new ArrayList<>();

			for (InterceptAccessWith interceptWith : interceptions) {
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

	private ConfiguredModel getDataModelOf(Access access) {
		String accessId = access.getAccessId();
		String modelName = AccessDomains.accessDataModelName(accessId);
		ConfiguredModel dataModel = configuredModels.byName(modelName);

		// TODO: work with reasons here
		return Objects.requireNonNull(dataModel, "Access configured data model '" + modelName + "' not found for access " + accessId);
	}

	private ConfiguredModel getServiceModelOf(Access access) {
		// The service domain exists, because it was created as part of the deploy
		ServiceDomain serviceDomain = serviceDomains.byId(access.getAccessId());
		return serviceDomain.configuredModel();
	}
}
