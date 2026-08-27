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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.resource.source.ResourceSource;

import hiconic.rx.access.model.md.InterceptAccessWith;
import hiconic.rx.access.model.md.PreEnrichResourceWith;
import hiconic.rx.access.module.api.AccessDataModelConfiguration;
import hiconic.rx.access.module.api.AccessInterceptorBuilder;
import hiconic.rx.access.module.api.ResourceEnricher;
import hiconic.rx.module.api.service.DelegatingModelConfiguration;
import hiconic.rx.module.api.service.ModelConfiguration;

public class RxAccessDataModelConfiguration implements AccessDataModelConfiguration, DelegatingModelConfiguration {

	private final ModelConfiguration modelConfiguration;
	private final List<AccessInterceptorEntry> interceptors = Collections.synchronizedList(new ArrayList<>());
	private volatile List<String> aspectOrdering = List.of();

	public RxAccessDataModelConfiguration(ModelConfiguration modelConfiguration) {
		this.modelConfiguration = modelConfiguration;
	}

	@Override
	public ModelConfiguration modelConfiguration() {
		return modelConfiguration;
	}

	// #################################################
	// ## . . . . . . . . Bind Aspect . . . . . . . . ##
	// #################################################

	@Override
	public AccessInterceptorBuilder bindAspect(String identification) {
		return new AccessInterceptorBuilder() {
			private String insertIdentification;
			private boolean before;

			@Override
			public void bind(Supplier<AccessAspect> interceptorSupplier) {
				AccessInterceptorEntry interceptorEntry = new AccessInterceptorEntry(identification, interceptorSupplier);
				register(interceptorEntry);
			}

			@Override
			@Deprecated
			public AccessInterceptorBuilder before(String identification) {
				this.insertIdentification = identification;
				this.before = true;
				return this;
			}

			@Override
			@Deprecated
			public AccessInterceptorBuilder after(String identification) {
				this.insertIdentification = identification;
				this.before = false;
				return this;
			}

			private void register(AccessInterceptorEntry interceptorEntry) {
				synchronized (interceptors) {
					if (insertIdentification == null) {
						interceptors.add(interceptorEntry);
					} else {
						int targetIndex = indexOfInterceptor(insertIdentification);
						if (targetIndex < 0)
							throw new NoSuchElementException("No access aspect found with identification: '" + insertIdentification + "'");
						interceptors.add(before ? targetIndex : targetIndex + 1, interceptorEntry);
					}

					if (interceptors.size() == 1)
						configureModel(RxAccessDataModelConfiguration.this::configureInterceptors);
				}
			}
		};
	}

	private int indexOfInterceptor(String identification) {
		for (int i = 0; i < interceptors.size(); i++)
			if (interceptors.get(i).identification().equals(identification))
				return i;
		return -1;
	}

	private void configureInterceptors(ModelMetaDataEditor editor) {
		List<AccessInterceptorEntry> orderedInterceptors = orderedInterceptors();
		int prio = orderedInterceptors.size();
		for (AccessInterceptorEntry entry : orderedInterceptors) {
			final InterceptAccessWith interceptWith = InterceptAccessWith.T.create();

			interceptWith.setAssociate(entry.interceptorSupplier.get());
			// CMD returns multi metadata by descending conflict priority. The first
			// declared aspect must therefore receive the highest priority.
			interceptWith.setConflictPriority((double) prio--);

			editor.addModelMetaData(interceptWith);
		}
	}

	private List<AccessInterceptorEntry> orderedInterceptors() {
		List<AccessInterceptorEntry> entries;
		synchronized (interceptors) {
			entries = new ArrayList<>(interceptors);
		}

		Map<String, AccessInterceptorEntry> byIdentification = new LinkedHashMap<>();
		for (AccessInterceptorEntry entry : entries) {
			if (byIdentification.putIfAbsent(entry.identification(), entry) != null)
				throw new IllegalStateException("Duplicate access aspect identification: '" + entry.identification() + "'");
		}

		List<AccessInterceptorEntry> result = new ArrayList<>(entries.size());
		Set<String> explicitlyOrdered = new LinkedHashSet<>();
		for (String identification : aspectOrdering) {
			AccessInterceptorEntry entry = byIdentification.get(identification);
			if (entry != null && explicitlyOrdered.add(identification))
				result.add(entry);
		}
		for (AccessInterceptorEntry entry : entries)
			if (!explicitlyOrdered.contains(entry.identification()))
				result.add(entry);

		return result;
	}

	@Override
	public void orderAspects(String... identifiers) {
		aspectOrdering = List.of(identifiers.clone());
	}

	private static record AccessInterceptorEntry(String identification, Supplier<AccessAspect> interceptorSupplier) {
	}

	// #################################################
	// ## . . . . Bind Resource Pre Enricher . . . . .##
	// #################################################

	@Override
	public void bindResourcePreEnricher(EntityType<? extends ResourceSource> sourceType, Supplier<ResourceEnricher> enricherSupplier) {
		configureModel(mdEditor -> {
			PreEnrichResourceWith md = PreEnrichResourceWith.T.create();
			md.setResourceEnricher(enricherSupplier.get());

			mdEditor.onEntityType(sourceType).addMetaData(md);
		});

	}

}
