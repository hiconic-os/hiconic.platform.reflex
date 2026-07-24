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
package hiconic.rx.access.smood.processing;

import java.util.UUID;

import com.braintribe.common.MutuallyExclusiveReadWriteLock;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.access.EmptyNonIncrementalAccess;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.generic.processing.IdGenerator;
import com.braintribe.model.processing.core.expert.impl.ConfigurableGmExpertRegistry;

import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.access.smood.model.configuration.SmoodAccess;
import hiconic.rx.module.api.service.ConfiguredModel;

public class SmoodAccessExpert implements AccessExpert<SmoodAccess> {

	@Override
	public Maybe<IncrementalAccess> deploy(SmoodAccess configuration, ConfiguredModel dataModel) {
		com.braintribe.model.access.smood.basic.SmoodAccess access = new com.braintribe.model.access.smood.basic.SmoodAccess();
		access.setAccessId(configuration.getAccessId());
		access.setModelName(dataModel.name());
		access.setDataDelegate(new EmptyNonIncrementalAccess(() -> dataModel.modelOracle().getGmMetaModel()));
		access.setReadWriteLock(new MutuallyExclusiveReadWriteLock());

		ConfigurableGmExpertRegistry expertRegistry = new ConfigurableGmExpertRegistry();
		expertRegistry.add(IdGenerator.class, String.class, entity -> UUID.randomUUID().toString());
		access.setExpertRegistry(expertRegistry);

		return Maybe.complete(access);
	}

}
