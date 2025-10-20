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
package hiconic.rx.cli.posix.parser;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

import hiconic.rx.cli.posix.parser.api.ParsedCommandLine;

public class ParsedCommandLineImpl implements ParsedCommandLine {

	private Set<GenericEntity> entities = new LinkedHashSet<>();
	
	@Override
	public void addEntity(GenericEntity entity) {
		entities.add(entity);
	}
	
	@Override
	public <O extends GenericEntity> Optional<O> findInstance(EntityType<O> optionsType) {
		return (Optional<O>) entities.stream().filter(optionsType::isInstance).findFirst();
	}
	
	@Override
	public <O extends GenericEntity> List<O> listInstances(EntityType<O> optionsType) {
		return (List<O>) entities.stream().filter(optionsType::isInstance).collect(Collectors.toList());
	}
}
