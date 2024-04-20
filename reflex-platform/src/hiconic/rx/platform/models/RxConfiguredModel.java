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
import java.util.function.Supplier;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.common.attribute.AttributeContext;
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
import com.braintribe.model.processing.service.api.MappingServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.impl.ServiceProcessors;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.model.service.processing.md.AroundProcessWith;
import hiconic.rx.model.service.processing.md.InterceptWith;
import hiconic.rx.model.service.processing.md.PostProcessWith;
import hiconic.rx.model.service.processing.md.PreProcessWith;
import hiconic.rx.model.service.processing.md.ProcessWith;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.InterceptorBuilder;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelReference;
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
	public CmdResolver cmdResolver(AttributeContext attributeContext) {
		return configuredModels.cmdResolver(attributeContext, modelOracle());
	}

	@Override
	public CmdResolver systemCmdResolver() {
		return configuredModels.systemCmdResolver(modelOracle());
	}
	
	@Override
	public ModelOracle modelOracle() {
		return cmdResolver().getModelOracle();
	}
	
	private void configureInterceptors(ModelMetaDataEditor editor) {
		int prio = 0;
		for (InterceptorEntry entry: interceptors) {
			final InterceptWith interceptWith;
			ServiceInterceptorProcessor processor = entry.interceptorSupplier.get();
			InterceptorKind kind = processor.getKind();
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
			
			RxInterceptor interceptor = new RxInterceptor(processor, entry.predicate);
			
			interceptWith.setAssociate(interceptor);
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
	public void addModel(ModelReference modelReference) {
		RxConfiguredModel configuredModel = configuredModels.acquire(modelReference);
		GmMetaModel gmMetaModel = configuredModel.configurationModelBuilder.get();
		addModel(gmMetaModel);
	}

	@Override
	public void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		cmdResolver.close();
		modelConfigurers.add(configurer);
	}
	
	@Override
	public <R extends ServiceRequest> void bindRequest(EntityType<R> requestType, Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier) {
		addModel(requestType.getModel());
		configureModel(editor -> editor.onEntityType(requestType).addMetaData(ProcessWith.create(serviceProcessorSupplier.get())));
	}
	
	@Override
	public <R extends ServiceRequest> void bindRequestMapped(EntityType<R> requestType,
			Supplier<MappingServiceProcessor<? super R, ?>> serviceProcessorSupplier) {
		addModel(requestType.getModel());
		configureModel(editor -> editor.onEntityType(requestType).addMetaData(ProcessWith.create(ServiceProcessors.dispatcher(serviceProcessorSupplier.get()))));
	}
	
	@Override
	public InterceptorBuilder bindInterceptor(String identification) {
		return new InterceptorBuilder() {
			private String insertIdentification;
			private boolean before;
			private EntityType<? extends ServiceRequest> requestType;
			private Predicate<ServiceRequest> predicate = r -> true;
			
			@Override
			public void bind(Supplier<ServiceInterceptorProcessor> interceptor) {
				InterceptorEntry interceptorEntry = new InterceptorEntry(identification, requestType, predicate, interceptor);
				register(interceptorEntry);
			}
			
			@Override
			public InterceptorBuilder predicate(Predicate<ServiceRequest> predicate) {
				this.predicate = predicate;
				return this;
			}
			
			@Override
			public InterceptorBuilder before(String identification) {
				this.insertIdentification = identification;
				this.before = true;
				return this;
			}
			
			@Override
			public InterceptorBuilder after(String identification) {
				this.insertIdentification = identification;
				this.before = false;
				return this;
			}
			
			@Override
			public InterceptorBuilder forType(EntityType<? extends ServiceRequest> requestType) {
				this.requestType = requestType;
				return this;
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
		};
	}
	
	private static class InterceptorEntry {
		String identification;
		Supplier<ServiceInterceptorProcessor> interceptorSupplier;
		EntityType<? extends ServiceRequest> requestType;
		Predicate<ServiceRequest> predicate;
		
		
		public InterceptorEntry(String identifier, EntityType<? extends ServiceRequest> requestType, Supplier<ServiceInterceptorProcessor> interceptorSupplier) {
			this(identifier, requestType, r -> true, interceptorSupplier);
		}
		
		public InterceptorEntry(String identifier, EntityType<? extends ServiceRequest> requestType, Predicate<ServiceRequest> predicate, Supplier<ServiceInterceptorProcessor> interceptorSupplier) {
			this.identification = identifier;
			this.predicate = predicate;
			this.interceptorSupplier = interceptorSupplier;
			this.requestType = requestType;
		}
	}
}
