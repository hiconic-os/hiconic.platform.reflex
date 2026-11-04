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
package hiconic.rx.platform.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.braintribe.model.processing.service.api.ServiceRequestContextualizer;
import com.braintribe.model.processing.service.api.ServiceRequestExecution;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceRequestContextualizerRegistry;

public class BasicServiceRequestContextualizerRegistry implements ServiceRequestContextualizerRegistry {

	private final List<ServiceRequestContextualizer> contextualizers = new CopyOnWriteArrayList<>();

	@Override
	public void register(ServiceRequestContextualizer contextualizer) {
		contextualizers.add(Objects.requireNonNull(contextualizer, "contextualizer must not be null"));
	}

	@Override
	public void contextualize(ServiceRequest request, ServiceRequestExecution execution) throws Exception {
		contextualize(List.copyOf(contextualizers), 0, request, execution);
	}

	private void contextualize(List<ServiceRequestContextualizer> snapshot, int index, ServiceRequest request, ServiceRequestExecution execution)
			throws Exception {
		if (index == snapshot.size()) {
			execution.execute();
			return;
		}

		ServiceRequestContextualizer contextualizer = snapshot.get(index);
		contextualizer.contextualize(request, () -> contextualize(snapshot, index + 1, request, execution));
	}
}
