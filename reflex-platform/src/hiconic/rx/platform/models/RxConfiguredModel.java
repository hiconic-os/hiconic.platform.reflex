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
package hiconic.rx.platform.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.gm._RootModel_;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.MappingServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.processing.service.api.ServicePostProcessor;
import com.braintribe.model.processing.service.api.ServicePreProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.impl.ServiceProcessors;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.OverridingPostProcessResponse;

import hiconic.rx.model.service.processing.md.AroundProcessWith;
import hiconic.rx.model.service.processing.md.InterceptWith;
import hiconic.rx.model.service.processing.md.ProcessWith;
import hiconic.rx.module.api.service.InterceptorBuilder;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelSymbol;
import hiconic.rx.module.api.service.ServiceProcessorRegistration;
import hiconic.rx.platform.service.RxInterceptor;

public class RxConfiguredModel extends AbstractRxConfiguredModel implements ModelConfiguration {

	private final String name;
	private final ConfigurationModelBuilder configurationModelBuilder;
	private final List<Consumer<ModelMetaDataEditor>> modelConfigurers = Collections.synchronizedList(new ArrayList<>());
	private final Set<GmMetaModel> models = new LinkedHashSet<>();
	private final List<InterceptorEntry> interceptors = Collections.synchronizedList(new ArrayList<>());
	private final List<InterceptorOrdering> interceptorOrderings = Collections.synchronizedList(new ArrayList<>());
	private volatile List<InterceptorOrdering> effectiveInterceptorOrderings;

	private boolean configuringModels = true;

	public RxConfiguredModel(RxConfiguredModels configuredModels, String modelName) {
		super(configuredModels);
		this.name = modelName;
		this.configurationModelBuilder = ConfigurationModels.create(modelName);
	}

	public RxConfiguredModel(RxConfiguredModels configuredModels, GmMetaModel model) {
		super(configuredModels);
		this.name = model.getName();
		this.configurationModelBuilder = ConfigurationModels.extend(model);

		this.models.addAll(model.getDependencies());
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public GmMetaModel gmModel() {
		GmMetaModel gmModel = configurationModelBuilder.get();
		if (gmModel.getDependencies().isEmpty())
			addModel(_RootModel_.reflection);
		return gmModel;
	}

	/**
	 * Applies all configurers registered via {@link ModelConfiguration#configureModel(Consumer)}. This includes those registered indirectly e.g. via
	 * {@link ModelConfiguration#bindRequest(EntityType, Supplier)}.
	 */
	/* package */ void finalizeModelConfiguration() {
		GmMetaModel gmModel = gmModel();

		BasicModelMetaDataEditor editor = new BasicModelMetaDataEditor(gmModel);
		for (Consumer<ModelMetaDataEditor> configurer : modelConfigurers)
			configurer.accept(editor);

		configuringModels = false;
	}

	@Override
	protected ModelOracle buildModelOracle() {
		if (configuringModels)
			throw new IllegalStateException("Cannot build ModelOracle at this phase, models are still being configured!");

		return new BasicModelOracle(gmModel());
	}

	private void configureInterceptors(ModelMetaDataEditor editor) {
		List<InterceptorEntry> entries;
		synchronized (interceptors) {
			entries = new ArrayList<>(interceptors);
		}

		Set<String> identifications = new LinkedHashSet<>();
		for (InterceptorEntry entry : entries)
			if (!identifications.add(entry.identification))
				throw new IllegalStateException("Duplicate service interceptor identification: '" + entry.identification + "'");

		int prio = entries.size();
		for (InterceptorEntry entry : entries) {
			ServiceInterceptorProcessor processor = entry.processor();
			InterceptWith interceptWith = AroundProcessWith.T.create();

			RxInterceptor interceptor = new RxInterceptor(entry.identification, processor.getKind(), asAroundProcessor(processor), entry.predicate);

			interceptWith.setAssociate(interceptor);
			// CMD returns multi metadata by descending conflict priority. The first
			// declared interceptor must therefore receive the highest priority.
			interceptWith.setConflictPriority((double) prio--);

			editor.onEntityType(entry.requestType).addMetaData(interceptWith);
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	static ServiceAroundProcessor<ServiceRequest, Object> asAroundProcessor(ServiceInterceptorProcessor processor) {
		switch (processor.getKind()) {
			case around:
				return (ServiceAroundProcessor<ServiceRequest, Object>) processor;
			case pre:
				ServicePreProcessor<ServiceRequest> preProcessor = (ServicePreProcessor<ServiceRequest>) processor;
				return (context, request, proceedContext) -> proceedContext.proceed(preProcessor.process(context, request));
			case post:
				ServicePostProcessor<Object> postProcessor = (ServicePostProcessor<Object>) processor;
				return (context, request, proceedContext) -> {
					Object response = proceedContext.proceed(request);
					Object postProcessorResponse = postProcessor.process(context, response);

					if (response != postProcessorResponse) {
						if (postProcessorResponse instanceof OverridingPostProcessResponse)
							response = ((OverridingPostProcessResponse) postProcessorResponse).getResponse();
						else
							response = postProcessorResponse;
					}

					return response;
				};
			default:
				throw new UnsupportedOperationException("Unsupported interception kind " + processor.getKind());
		}
	}

	/* package */ void finalizeInterceptorOrderings() {
		List<InterceptorOrdering> orderings;
		orderings = new ArrayList<>();
		modelOracle().getDependencies().includeSelf().transitive().asGmMetaModels().forEach(model -> {
			RxConfiguredModel configuredModel = configuredModels.findConfiguredModel(model);
			if (configuredModel != null)
				orderings.addAll(configuredModel.localInterceptorOrderings());
		});
		effectiveInterceptorOrderings = List.copyOf(orderings);
	}

	private List<InterceptorOrdering> localInterceptorOrderings() {
		synchronized (interceptorOrderings) {
			return new ArrayList<>(interceptorOrderings);
		}
	}

	public List<RxInterceptor> orderedInterceptors(List<RxInterceptor> interceptors) {
		List<RxInterceptor> legacyCompatibleOrder = legacyCompatibleOrder(interceptors);
		Map<String, RxInterceptor> byIdentification = new LinkedHashMap<>();
		for (RxInterceptor interceptor : legacyCompatibleOrder) {
			String identification = interceptor.identification();
			if (identification == null)
				throw new IllegalStateException("Service interceptor without identification: " + interceptor.processor());
			if (byIdentification.putIfAbsent(identification, interceptor) != null)
				throw new IllegalStateException("Duplicate effective service interceptor identification: '" + identification + "'");
		}

		List<InterceptorOrdering> orderings = effectiveInterceptorOrderings;
		if (orderings == null)
			orderings = localInterceptorOrderings();

		return applyExplicitOrderings(legacyCompatibleOrder, byIdentification, orderings);
	}

	private static List<RxInterceptor> legacyCompatibleOrder(List<RxInterceptor> interceptors) {
		List<RxInterceptor> pre = new ArrayList<>();
		List<RxInterceptor> around = new ArrayList<>();
		List<RxInterceptor> post = new ArrayList<>();

		for (RxInterceptor interceptor : interceptors) {
			switch (interceptor.originalKind()) {
				case pre:
					pre.add(interceptor);
					break;
				case around:
					around.add(interceptor);
					break;
				case post:
					post.add(interceptor);
					break;
				default:
					throw new UnsupportedOperationException("Unsupported interception kind");
			}
		}

		// Post processors become around processors and therefore execute on unwind.
		// Reversing their nesting keeps their former registration/execution order.
		Collections.reverse(post);

		List<RxInterceptor> result = new ArrayList<>(interceptors.size());
		result.addAll(pre);
		result.addAll(post);
		result.addAll(around);
		return result;
	}

	private static List<RxInterceptor> applyExplicitOrderings(List<RxInterceptor> baseline,
			Map<String, RxInterceptor> byIdentification, List<InterceptorOrdering> orderings) {
		Map<String, Set<String>> successors = new HashMap<>();
		Map<String, Integer> predecessorCounts = new HashMap<>();
		Map<String, Integer> baselineIndexes = new HashMap<>();

		for (int i = 0; i < baseline.size(); i++) {
			String identification = baseline.get(i).identification();
			successors.put(identification, new LinkedHashSet<>());
			predecessorCounts.put(identification, 0);
			baselineIndexes.put(identification, i);
		}

		for (InterceptorOrdering ordering : orderings) {
			if (!byIdentification.containsKey(ordering.before) || !byIdentification.containsKey(ordering.after))
				continue;
			if (successors.get(ordering.before).add(ordering.after))
				predecessorCounts.compute(ordering.after, (key, count) -> count + 1);
		}

		PriorityQueue<String> ready = new PriorityQueue<>(Comparator.comparingInt(baselineIndexes::get));
		predecessorCounts.forEach((identification, count) -> {
			if (count == 0)
				ready.add(identification);
		});

		List<RxInterceptor> result = new ArrayList<>(baseline.size());
		while (!ready.isEmpty()) {
			String identification = ready.remove();
			result.add(byIdentification.get(identification));
			for (String successor : successors.get(identification)) {
				int remaining = predecessorCounts.compute(successor, (key, count) -> count - 1);
				if (remaining == 0)
					ready.add(successor);
			}
		}

		if (result.size() != baseline.size()) {
			Set<String> cyclic = new LinkedHashSet<>();
			predecessorCounts.forEach((identification, count) -> {
				if (count > 0)
					cyclic.add(identification);
			});
			throw new IllegalStateException("Cyclic service interceptor ordering involving: " + cyclic);
		}

		return result;
	}

	@Override
	public void addModel(ArtifactReflection modelArtifactReflection) {
		addModelByName(modelArtifactReflection.name());
	}

	@Override
	public void addModelByName(String modelName) {
		Model model = GMF.getTypeReflection().findModel(modelName);

		if (model != null) {
			addModel(model);
			return;
		}

		AbstractRxConfiguredModel configuredModel = configuredModels.byName(modelName);
		if (configuredModel == null)
			throw new IllegalArgumentException("No model found with name: " + modelName);

		addRxModel(configuredModel);
	}

	@Override
	public void addModel(Model model) {
		addModel((GmMetaModel) model.getMetaModel());
	}

	@Override
	public void addModel(ModelSymbol modelReference) {
		addRxModel(configuredModels.acquire(modelReference));
	}

	private void addRxModel(AbstractRxConfiguredModel configuredModel) {
		addModel(configuredModel.gmModel());
	}

	@Override
	public void addModel(GmMetaModel gmModel) {
		if (!configuringModels)
			throwCannotConfigureModelException("Cannot add model [" + gmModel.getName() + "]");

		synchronized (models) {
			if (models.add(gmModel))
				configurationModelBuilder.addDependency(gmModel);
		}
	}

	@Override
	public void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		if (!configuringModels)
			throwCannotConfigureModelException("Cannot configure model");

		modelConfigurers.add(configurer);
	}

	private void throwCannotConfigureModelException(String problem) {
		throw new IllegalStateException(
				problem + " after model configuration has been finalized. Model can only be configured before onDeploy() phase.");
	}

	@Override
	public <R extends ServiceRequest> void bindRequest(EntityType<R> requestType, Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier) {
		bindRequest(requestType, serviceProcessorSupplier, 0d);
	}

	@Override
	public <R extends ServiceRequest> void bindRequest(EntityType<R> requestType, Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier,
			double conflictPriority) {
		addModelIfNotNull(requestType.getModel());
		configureModel(editor -> {
			ProcessWith processWith = ProcessWith.create(serviceProcessorSupplier.get());
			processWith.setConflictPriority(conflictPriority);
			editor.onEntityType(requestType).addMetaData(processWith);
		});
	}

	@Override
	public <R extends ServiceRequest> void bindRequest(EntityType<R> requestType, String serviceProcessorKey) {
		bindRequest(requestType, () -> lazyRegisteredProcessor(serviceProcessorKey));
	}

	@SuppressWarnings("rawtypes")
	private <R extends ServiceRequest> ServiceProcessor<? super R, ?> lazyRegisteredProcessor(String serviceProcessorKey) {
		return (context, request) -> {
			ServiceProcessorRegistration registration = configuredModels.serviceProcessorRegistry().require(serviceProcessorKey);
			ServiceProcessor processor = registration.processorSupplier().get();
			return processor.process(context, request);
		};
	}

	@Override
	public <R extends ServiceRequest> void bindRequestMapped( //
			EntityType<R> requestType, Supplier<MappingServiceProcessor<? super R, ?>> serviceProcessorSupplier) {

		bindRequest(requestType, () -> ServiceProcessors.dispatcher(serviceProcessorSupplier.get()));
	}

	/* package */ void addModelIfNotNull(Model model) {
		if (model != null)
			addModel(model);
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
			@Deprecated
			public InterceptorBuilder before(String identification) {
				this.insertIdentification = identification;
				this.before = true;
				return this;
			}

			@Override
			@Deprecated
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

			private void register(InterceptorEntry interceptorEntry) {
				addModelIfNotNull(interceptorEntry.requestType.getModel());

				synchronized (interceptors) {
					interceptors.add(interceptorEntry);

					if (interceptors.size() == 1)
						configureModel(RxConfiguredModel.this::configureInterceptors);
				}

				if (insertIdentification != null)
					addOrdering(before ? interceptorEntry.identification : insertIdentification,
							before ? insertIdentification : interceptorEntry.identification);
			}
		};
	}

	@Override
	public void orderInterceptors(String... identifications) {
		for (int i = 1; i < identifications.length; i++)
			addOrdering(identifications[i - 1], identifications[i]);
	}

	private void addOrdering(String before, String after) {
		if (before == null || after == null)
			throw new IllegalArgumentException("Service interceptor ordering identifiers must not be null");
		if (before.equals(after))
			throw new IllegalArgumentException("A service interceptor cannot be ordered relative to itself: '" + before + "'");
		interceptorOrderings.add(new InterceptorOrdering(before, after));
	}

	private static class InterceptorOrdering {
		final String before;
		final String after;

		InterceptorOrdering(String before, String after) {
			this.before = before;
			this.after = after;
		}
	}

	private static class InterceptorEntry {
		String identification;
		Supplier<ServiceInterceptorProcessor> interceptorSupplier;
		EntityType<? extends ServiceRequest> requestType;
		Predicate<ServiceRequest> predicate;
		private ServiceInterceptorProcessor processor;

		public InterceptorEntry(String identifier, EntityType<? extends ServiceRequest> requestType, Predicate<ServiceRequest> predicate,
				Supplier<ServiceInterceptorProcessor> interceptorSupplier) {

			this.identification = identifier;
			this.predicate = predicate;
			this.interceptorSupplier = interceptorSupplier;
			this.requestType = requestType;
		}

		ServiceInterceptorProcessor processor() {
			if (processor == null)
				processor = interceptorSupplier.get();
			return processor;
		}
	}
}
