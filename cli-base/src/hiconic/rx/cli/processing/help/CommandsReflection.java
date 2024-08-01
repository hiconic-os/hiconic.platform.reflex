// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.cli.processing.help;

import static com.braintribe.utils.lcd.CollectionTools2.newList;
import static com.braintribe.utils.lcd.StringTools.camelCaseToSocialDistancingCase;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.GmProperty;
import com.braintribe.model.meta.data.mapping.Alias;
import com.braintribe.model.meta.data.prompt.Visible;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.platform.cli.model.api.From;
import hiconic.rx.platform.cli.model.api.Options;

/**
 * @author peter.gazdik
 */
public class CommandsReflection {

	public static class CommandsOverview {
		public final List<GmEntityType> requestTypes;
		public final List<GmEntityType> inputTypes;
		public final List<GmEntityType> extraTypes; // for now just options

		public final List<GmEntityType> allTypes;

		private CommandsOverview(List<GmEntityType> requestTypes, List<GmEntityType> inputTypes, List<GmEntityType> extraTypes) {
			this.requestTypes = requestTypes;
			this.inputTypes = inputTypes;
			this.extraTypes = extraTypes;

			this.allTypes = newList();
			this.allTypes.addAll(requestTypes);
			this.allTypes.addAll(inputTypes);
			this.allTypes.addAll(extraTypes);
		}

	}

	public final ModelOracle modelOracle;
	public final ModelMdResolver mdResolver;

	public final Comparator<GmEntityType> entityComparator_StandardAlias;

	public CommandsReflection(ModelOracle modelOracle, ModelMdResolver mdResolver) {
		this.modelOracle = modelOracle;
		this.mdResolver = mdResolver;

		this.entityComparator_StandardAlias = Comparator.comparing(this::resolveStandardAlias);
	}

	public CommandsOverview getCommandsOverview() {
		EntityTypeOracle serviceRequestOracle = modelOracle.findEntityTypeOracle(ServiceRequest.T);
		EntityTypeOracle fromOracle = modelOracle.findEntityTypeOracle(From.T);
		EntityTypeOracle optionsOracle = modelOracle.findEntityTypeOracle(Options.T);

		GmMetaModel serviceApiModel = serviceRequestOracle != null? serviceRequestOracle.asGmEntityType().declaringModel() : null;

		List<GmEntityType> requestTypes = listTypes(serviceRequestOracle, serviceApiModel);
		List<GmEntityType> inputTypes = listTypes(fromOracle, null);
		List<GmEntityType> extraTypes = listTypes(optionsOracle, null);

		return new CommandsOverview(requestTypes, inputTypes, extraTypes);
	}

	private List<GmEntityType> listTypes(EntityTypeOracle serviceRequestOracle, GmMetaModel exclusionModel) {
		if (serviceRequestOracle == null)
			return Collections.emptyList();
		
		return serviceRequestOracle.getSubTypes() //
				.transitive() //
				.includeSelf() //
				.onlyInstantiable() //
				.asGmTypes() //
				.stream() //
				.map(t -> (GmEntityType) t) //
				.filter(t -> t.getDeclaringModel() != exclusionModel) //
				.filter(this::isEntityTypeVisible) //
				.sorted(entityComparator_StandardAlias) //
				.collect(Collectors.toList());
	}

	private boolean isEntityTypeVisible(GmEntityType gmType) {
		return mdResolver.entityType(gmType).is(Visible.T);
	}

	/////////////

	/** Returns the first element from {@link #nameAndAliasesSorted(GmEntityType)}. */
	public String resolveStandardAlias(GmEntityType gmType) {
		return nameAndAliasesSorted(gmType) //
				.findFirst() //
				.get(); // must return at least one result as every type has at least it's own name, if no alias
	}

	/**
	 * Returns a stream of the {@link #toCommandName(GmEntityType) natural command name} and all it's configured aliases sorted primarily from
	 * shortest, secondarily alphabetically.
	 * <p>
	 * For example for "jinni-options" and it's alias "options", the order would be ["options", "jinni-options"].
	 * <p>
	 * Note that we consider the first element in the stream as the {@link #resolveStandardAlias(GmEntityType) standard} alias, and that is the alias
	 * displayed e.g. in "launch-script help" output.
	 */
	public Stream<String> nameAndAliasesSorted(GmEntityType gmType) {
		List<Alias> aliases = mdResolver.entityType(gmType).meta(Alias.T).list();

		return Stream.concat( //
				Stream.of(toCommandName(gmType)), //
				aliases.stream() //
						.map(Alias::getName) //
		).sorted(StringTools::compareStringsSizeFirst);
	}

	/////////////

	public GmEntityType resolveTypeFromCommandName(String commandName) {
		EntityTypeOracle entityTypeOracle = modelOracle.findEntityTypeOracle(GenericEntity.T);

		Set<GmEntityType> gmTypes = entityTypeOracle.getSubTypes() //
				.transitive() //
				.includeSelf() //
				.onlyInstantiable() //
				.asGmTypes();

		for (GmEntityType gmEntityType : gmTypes) {
			EntityType<?> entityType = gmEntityType.reflectionType();
			String shortcut = CommandsReflection.toCommandName(gmEntityType);

			if (commandName.equals(shortcut))
				return gmEntityType;

			List<Alias> aliases = mdResolver.entityType(entityType).meta(Alias.T).list();

			for (Alias alias : aliases)
				if (commandName.equals(alias.getName()))
					return gmEntityType;
		}

		return null;
	}

	/////////////

	public List<GmProperty> getRelevantPropertiesOf(GmEntityType gmType) {
		return new RelevantPropertyResolver(gmType).resolvePlease();
	}

	private class RelevantPropertyResolver {
		private final GmEntityType requestType;
		private final Predicate<GmProperty> propertyFilter;
		private final EntityMdResolver requestTypeMdResolver;

		private final GmMetaModel rootModel = declaringModelOf(GenericEntity.T);
		private final GmMetaModel serviceApiModel = declaringModelOf(ServiceRequest.T);

		public RelevantPropertyResolver(GmEntityType requestType) {
			this.requestType = requestType;
			this.requestTypeMdResolver = mdResolver.entityType(requestType);
			this.propertyFilter = isServiceRequest() ? this::notDeclaredInRootOrServiceApi : this::notDeclaredInRoot;
		}

		private GmMetaModel declaringModelOf(EntityType<?> et) {
			return modelOracle.findEntityTypeOracle(et).asGmEntityType().getDeclaringModel();
		}

		private boolean isServiceRequest() {
			return ServiceRequest.T.isAssignableFrom(requestType.reflectionType());
		}

		private boolean notDeclaredInRootOrServiceApi(GmProperty p) {
			GmMetaModel declaringModel = p.declaringModel();
			return declaringModel != rootModel && declaringModel != serviceApiModel;
		}

		private boolean notDeclaredInRoot(GmProperty p) {
			return p.declaringModel() != rootModel;
		}

		public List<GmProperty> resolvePlease() {
			return modelOracle.getEntityTypeOracle(requestType) //
					.getProperties() //
					.asGmProperties() //
					.filter(propertyFilter) //
					.filter(this::isPropertyVisible) //
					.sorted(Comparator.comparing(GmProperty::getName)) //
					.collect(Collectors.toList());
		}

		private boolean isPropertyVisible(GmProperty p) {
			return requestTypeMdResolver.property(p).is(Visible.T);
		}

	}

	/////////////

	public String cliNameOf(GmProperty gmProperty) {
		return toCliArgumentName(gmProperty.getName());
	}

	public Stream<String> cliNameAndAliasesOf(GmProperty gmProperty) {
		List<Alias> aliases = mdResolver.property(gmProperty).meta(Alias.T).list();

		return Stream.concat( //
				Stream.of(gmProperty.getName()), //
				aliases.stream().map(Alias::getName) //
		) //
				.map(this::toCliArgumentName);
	}

	private String toCliArgumentName(String propertyOrAlias) {
		return propertyOrAlias.length() == 1 ? "-" + propertyOrAlias : "--" + propertyOrAlias;
	}

	/////////////

	public static String toCommandName(GmEntityType type) {
		return camelCaseToSocialDistancingCase(toShortName(type));
	}

	private static String toShortName(GmEntityType type) {
		return type.<EntityType<?>> reflectionType().getShortName();
	}
}
