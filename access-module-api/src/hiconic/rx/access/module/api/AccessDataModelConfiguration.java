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

import java.util.function.Supplier;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.source.ResourceSource;

import hiconic.rx.module.api.service.ModelConfiguration;

public interface AccessDataModelConfiguration extends ModelConfiguration {

	AccessInterceptorBuilder bindAspect(String identifier);

	default AccessInterceptorBuilder bindAspect(AccessAspectSymbol identifier) {
		return bindAspect(identifier.name());
	}

	/**
	 * Declares the execution order independently from aspect registration. A later call replaces the previously declared order. Identifiers which
	 * are not registered on this configured model are ignored; registered aspects not mentioned here retain their registration order behind the
	 * explicitly ordered aspects.
	 */
	void orderAspects(String... identifiers);

	default void orderAspects(AccessAspectSymbol... identifiers) {
		String[] names = new String[identifiers.length];
		for (int i = 0; i < identifiers.length; i++)
			names[i] = identifiers[i].name();
		orderAspects(names);
	}

	void bindResourcePreEnricher(EntityType<? extends ResourceSource> sourceType, Supplier<ResourceEnricher> enricherSupplier);

}
