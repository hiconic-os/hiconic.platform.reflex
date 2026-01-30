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

import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.generic.reflection.EntityType;

import hiconic.rx.access.api.AccessRequestProcessor;
import hiconic.rx.module.api.service.ModelConfiguration;

public interface AccessModelConfiguration extends ModelConfiguration {

	AccessInterceptorBuilder bindAspect(String identifier);

	<R extends AccessRequest> void bindAccessRequest(EntityType<R> request, Supplier<AccessRequestProcessor<? super R, ?>> processorSupplier);

}
