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
package hiconic.rx.cli.processing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class EntityFactory implements Function<EntityType<?>, GenericEntity> {
	private Map<EntityType<?>, GenericEntity> singletons = new HashMap<>();
	
	public <S extends GenericEntity> void registerSingleton(EntityType<S> singletonType, S singleton) {
		singletons.put(singletonType, singleton);
	}
	
	@Override
	public GenericEntity apply(EntityType<?> type) {
		GenericEntity entity = singletons.get(type);
		
		if (entity != null)
			return entity;
		
		return type.create();
	}
}
