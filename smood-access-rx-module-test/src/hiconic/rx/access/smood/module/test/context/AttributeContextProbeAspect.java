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
package hiconic.rx.access.smood.module.test.context;

import java.util.concurrent.atomic.AtomicReference;

import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.aop.api.aspect.AccessJoinPoint;
import com.braintribe.model.processing.aop.api.aspect.PointCutConfigurationContext;
import com.braintribe.model.processing.aop.api.context.AroundContext;
import com.braintribe.model.processing.aop.api.interceptor.AroundInterceptor;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.query.EntityQueryResult;
import com.braintribe.utils.collection.impl.AttributeContexts;

public class AttributeContextProbeAspect implements AccessAspect, AroundInterceptor<EntityQuery, EntityQueryResult> {
	private final AtomicReference<String> observedMarker = new AtomicReference<>();

	@Override
	public void configurePointCuts(PointCutConfigurationContext context) {
		context.addPointCutBinding(AccessJoinPoint.queryEntities, this);
	}

	@Override
	public EntityQueryResult run(AroundContext<EntityQuery, EntityQueryResult> context) {
		observedMarker.set(AttributeContexts.peek().findOrNull(AttributeContextMarker.class));
		return context.proceed();
	}

	public void reset() {
		observedMarker.set(null);
	}

	public String observedMarker() {
		return observedMarker.get();
	}
}
