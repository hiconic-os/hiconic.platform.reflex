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

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.data.mapping.Alias;
import com.braintribe.model.meta.data.mapping.PositionalArguments;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.utils.lcd.StringTools;

public class CommandLineParserTypeIndex implements AutoCloseable {
	private final Map<EntityType<?>, Map<String, Property>> propertyNameIndex = new HashMap<>();
	private final LazyInitialized<Map<String, EntityType<?>>> shortcuts = new LazyInitialized<>(this::buildShortcuts);
	private final CmdResolver cmdResolver; 
	
	public CommandLineParserTypeIndex(CmdResolver cmdResolver) {
		this.cmdResolver = cmdResolver;
	}
	
	@Override
	public void close() throws Exception {
		// NOOP
	}
	
	/*package*/ Property findProperty(EntityType<?> entityType, String identifier) {
		return propertyNameIndex.computeIfAbsent(entityType, this::generateIndex).get(identifier);
	}
	
	/* package */ List<Property> getPositionalArguments(EntityType<?> entityType) {
		PositionalArguments positionalArguments = cmdResolver.getMetaData() //
				.entityType(entityType) //
				.meta(PositionalArguments.T) //
				.exclusive();

		if (positionalArguments == null)
			return Collections.emptyList();

		List<String> propNames = positionalArguments.getProperties();

		List<Property> result = newList(propNames.size());

		for (String propName : propNames) {
			Property property = entityType.findProperty(propName);
			if (property == null) {
				// TODO this won't be seen, as there is no console at this point. 
				println(yellow("WARNING: Positional argument '" + propName + "' not found for type " + entityType.getShortName()));
				return Collections.emptyList();
			}

			result.add(property);
		}

		return result;
	}
	
	private Map<String, Property> generateIndex(EntityType<?> entityType) {
		Map<String, Property> properties = new HashMap<>();
		for (Property property: entityType.getProperties()) {
			String name = property.getName();
			properties.put(name, property);
			
			List<Alias> aliases = cmdResolver.getMetaData().useCase("command-line").property(property).meta(Alias.T).list();
			for (Alias alias: aliases) {
				properties.put(alias.getName(), property);
			}
		}
		
		return properties;
	}
	
	public EntityType<?> findEntityType(String identifier) {
		EntityTypeOracle entityTypeOracle = cmdResolver.getModelOracle().findEntityTypeOracle(identifier);
		if (entityTypeOracle != null) 
			return entityTypeOracle.asType();

		return shortcuts.get().get(identifier);
	}
	
	public Map<String, EntityType<?>> buildShortcuts() {
		Map<String, EntityType<?>> bean = new LinkedHashMap<>();
		
		ModelOracle oracle = cmdResolver.getModelOracle();
		EntityTypeOracle entityTypeOracle = oracle.findEntityTypeOracle(GenericEntity.T);
		
		Set<GmEntityType> types = entityTypeOracle
			.getSubTypes()
			.transitive()
			.includeSelf()
			.onlyInstantiable()
			.asGmTypes();
		
		for (GmEntityType type: types) {
			EntityType<?> reflectionType = type.reflectionType();
			String shortcut = StringTools.splitCamelCase(reflectionType.getShortName()).stream().map(String::toLowerCase).collect(Collectors.joining("-"));
			bean.put(shortcut, reflectionType);
			for (Alias alias: cmdResolver.getMetaData().entityType(type).meta(Alias.T).list()) {
				String aliasName = alias.getName();
				
				if (aliasName != null)
					bean.put(aliasName, reflectionType);
			}
		}

		return bean;
	}
}
