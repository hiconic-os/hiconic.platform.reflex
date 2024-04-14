package hiconic.rx.platform.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.gm._RootModel_;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.InterceptorKind;
import com.braintribe.model.processing.service.api.InterceptorRegistration;
import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.model.service.processing.md.AroundProcessWith;
import hiconic.rx.model.service.processing.md.InterceptWith;
import hiconic.rx.model.service.processing.md.PostProcessWith;
import hiconic.rx.model.service.processing.md.PreProcessWith;
import hiconic.rx.model.service.processing.md.ProcessWith;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ConfiguredModelReference;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.platform.service.RxInterceptor;

public class RxConfiguredModel implements ModelConfiguration, ConfiguredModel {
	private String name;
	private ConfigurationModelBuilder configurationModelBuilder;
	private List<Consumer<ModelMetaDataEditor>> modelConfigurers = Collections.synchronizedList(new ArrayList<>());
	private Set<GmMetaModel> models = new LinkedHashSet<>();
	private List<InterceptorEntry> interceptors = Collections.synchronizedList(new ArrayList<>());
	private LazyInitialized<CmdResolver> cmdResolver = new LazyInitialized<>(this::configureModel);
	private RxConfiguredModels configuredModels;
	
	public RxConfiguredModel(RxConfiguredModels configuredModels, String modelName) {
		this.name = modelName;
		this.configuredModels = configuredModels;
		configurationModelBuilder = ConfigurationModels.create(modelName);;
	}
	
	@Override
	public String modelName() {
		return name;
	}
	
	@Override
	public <R extends ServiceRequest> void register(EntityType<R> requestType,
			ServiceProcessor<? super R, ?> serviceProcessor) {
		addModel(requestType.getModel());
		configureModel(editor -> editor.onEntityType(requestType).addMetaData(ProcessWith.create(serviceProcessor)));
	}
	
	private CmdResolver configureModel() {
		GmMetaModel gmMetaModel = configurationModelBuilder.get();
		if (gmMetaModel.getDependencies().isEmpty())
			addModel(_RootModel_.reflection);
			
		BasicModelMetaDataEditor editor = new BasicModelMetaDataEditor(gmMetaModel);

		for (Consumer<ModelMetaDataEditor> configurer : modelConfigurers) {
			configurer.accept(editor);
		}
		
		BasicModelOracle oracle = new BasicModelOracle(gmMetaModel);
		CmdResolver cmdResolver = CmdResolverImpl.create(oracle).done();
		return cmdResolver;
	}
	
	@Override
	public CmdResolver cmdResolver() {
		return cmdResolver.get();
	}
	
	@Override
	public ModelOracle modelOracle() {
		return cmdResolver().getModelOracle();
	}
	
	@Override
	public InterceptorRegistration registerInterceptor(String identification) {
		return new InterceptorRegistration() {
			
			private String insertIdentification;
			private boolean before;
			
			@Override
			public void register(ServiceInterceptorProcessor interceptor) {
				registerForType(ServiceRequest.T, interceptor);
			}
			
			@Override
			public <R extends ServiceRequest> void registerForType(EntityType<R> requestType, ServiceInterceptorProcessor interceptor) {
				InterceptorEntry interceptorEntry = new InterceptorEntry(identification, requestType, interceptor);
				register(interceptorEntry);
			}
			
			private <R extends ServiceRequest> void register(InterceptorEntry interceptorEntry) {
				addModel(interceptorEntry.requestType.getModel());
				synchronized (interceptors) {
					if (insertIdentification != null) {
						requireInterceptorIterator(insertIdentification, before).add(interceptorEntry);
					}
					else {
						interceptors.add(interceptorEntry);
					}
					
					if (interceptors.size() == 1)
						configureModel(RxConfiguredModel.this::configureInterceptors);
				}
			}
			
			@Override
			public void registerWithPredicate(Predicate<ServiceRequest> predicate, ServiceInterceptorProcessor interceptor) {
				InterceptorEntry interceptorEntry = new InterceptorEntry(identification, ServiceRequest.T, predicate, interceptor);
				register(interceptorEntry);
			}
			
			@Override
			public InterceptorRegistration before(String identification) {
				this.insertIdentification = identification;
				this.before = true;
				return this;
			}
			
			@Override
			public InterceptorRegistration after(String identification) {
				this.insertIdentification = identification;
				this.before = false;
				return this;
			}
		};
	}
	
	private void configureInterceptors(ModelMetaDataEditor editor) {
		int prio = 0;
		for (InterceptorEntry entry: interceptors) {
			final InterceptWith interceptWith;
			InterceptorKind kind = entry.interceptor.processor().getKind();
			switch (kind) {
			case around:
				interceptWith = AroundProcessWith.T.create();
				break;
			case post:
				interceptWith = PostProcessWith.T.create();
				break;
			case pre:
				interceptWith = PreProcessWith.T.create();
				break;
			default:
				throw new UnsupportedOperationException("Unsupported interception kind " + kind);
			}
			
			interceptWith.setAssociate(entry.interceptor);
			interceptWith.setConflictPriority((double)prio++);
			
			editor.onEntityType(entry.requestType).addMetaData(interceptWith);
		}
	}
	
	private ListIterator<InterceptorEntry> requireInterceptorIterator(String identification, boolean before) {
		ListIterator<InterceptorEntry> iterator = find(identification, before);
		
		if (!iterator.hasNext())
			throw new NoSuchElementException("No processor found with identification: '" + identification + "'");
		
		return iterator;
	}

	private ListIterator<InterceptorEntry> find(String identification, boolean before) {
		ListIterator<InterceptorEntry> it = interceptors.listIterator();
		while (it.hasNext()) {
			InterceptorEntry entry = it.next();
			if (entry.identification.equals(identification)) {
				if (before)
					it.previous();
				break;
			}
		}
		
		return it;
	}
	
	@Override
	public void addModel(GmMetaModel gmModel) {
		synchronized (models) {
			if (models.add(gmModel))
				configurationModelBuilder.addDependency(gmModel);
		}
	}

	@Override
	public void addModel(ArtifactReflection modelArtifactReflection) {
		addModelByName(modelArtifactReflection.name());
	}
	
	@Override
	public void addModel(Model model) {
		addModel((GmMetaModel)model.getMetaModel());
	}
	
	@Override
	public void addModelByName(String modelName) {
		addModel(GMF.getTypeReflection().getModel(modelName));
	}
	
	@Override
	public void addModel(ConfiguredModelReference modelReference) {
		RxConfiguredModel configuredModel = configuredModels.acquire(modelReference);
		GmMetaModel gmMetaModel = configuredModel.configurationModelBuilder.get();
		addModel(gmMetaModel);
	}

	@Override
	public void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		modelConfigurers.add(configurer);
	}
	
	private static class InterceptorEntry {
		String identification;
		RxInterceptor interceptor;
		EntityType<? extends ServiceRequest> requestType;
		
		public InterceptorEntry(String identifier, EntityType<? extends ServiceRequest> requestType, ServiceInterceptorProcessor interceptor) {
			this(identifier, requestType, r -> true, interceptor);
		}
		
		public InterceptorEntry(String identifier, EntityType<? extends ServiceRequest> requestType, Predicate<ServiceRequest> predicate, ServiceInterceptorProcessor interceptor) {
			this.identification = identifier;
			this.interceptor = new RxInterceptor(interceptor, predicate);
			this.requestType = requestType;
		}
	}
}
