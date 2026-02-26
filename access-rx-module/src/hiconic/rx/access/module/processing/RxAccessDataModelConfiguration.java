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
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
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
			public AccessInterceptorBuilder before(String identification) {
				this.insertIdentification = identification;
				this.before = true;
				return this;
			}

			@Override
			public AccessInterceptorBuilder after(String identification) {
				this.insertIdentification = identification;
				this.before = false;
				return this;
			}

			private void register(AccessInterceptorEntry interceptorEntry) {
				synchronized (interceptors) {
					if (insertIdentification != null) {
						requireInterceptorIterator(insertIdentification, before).add(interceptorEntry);
					} else {
						interceptors.add(interceptorEntry);
					}

					if (interceptors.size() == 1)
						configureModel(RxAccessDataModelConfiguration.this::configureInterceptors);
				}
			}
		};
	}

	private ListIterator<AccessInterceptorEntry> requireInterceptorIterator(String identification, boolean before) {
		ListIterator<AccessInterceptorEntry> iterator = find(identification, before);

		if (!iterator.hasNext())
			throw new NoSuchElementException("No processor found with identification: '" + identification + "'");

		return iterator;
	}

	private ListIterator<AccessInterceptorEntry> find(String identification, boolean before) {
		ListIterator<AccessInterceptorEntry> it = interceptors.listIterator();
		while (it.hasNext()) {
			AccessInterceptorEntry entry = it.next();
			if (entry.identification().equals(identification)) {
				if (before)
					it.previous();
				break;
			}
		}

		return it;
	}

	private void configureInterceptors(ModelMetaDataEditor editor) {
		int prio = 0;
		for (AccessInterceptorEntry entry : interceptors) {
			final InterceptAccessWith interceptWith = InterceptAccessWith.T.create();

			interceptWith.setAssociate(entry.interceptorSupplier.get());
			interceptWith.setConflictPriority((double) prio++);

			editor.addModelMetaData(interceptWith);
		}
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
