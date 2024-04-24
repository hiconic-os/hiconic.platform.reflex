// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package hiconic.rx.cli.processing.help;

import static com.braintribe.console.ConsoleOutputs.brightBlack;
import static com.braintribe.console.ConsoleOutputs.brightWhite;
import static com.braintribe.console.ConsoleOutputs.configurableSequence;
import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.model.service.api.result.Neutral.NEUTRAL;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.console.output.ConsoleOutputContainer;
import com.braintribe.model.generic.i18n.LocalizedString;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.value.EnumReference;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmProperty;
import com.braintribe.model.meta.data.constraint.Mandatory;
import com.braintribe.model.meta.data.mapping.Alias;
import com.braintribe.model.meta.data.mapping.PositionalArguments;
import com.braintribe.model.meta.data.prompt.Deprecated;
import com.braintribe.model.meta.data.prompt.Description;
import com.braintribe.model.meta.data.prompt.Hidden;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.PropertyMdResolver;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.cli.processing.help.CommandsReflection.CommandsOverview;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.platform.cli.model.api.Help;

public class HelpProcessor implements ServiceProcessor<Help, Neutral> {
	/** In conjunction with {@link Hidden} marks a request to be omitted from the {@link Help} output. */
	public static final String USE_CASE_HELP = "help";
	
	/**
	 * In conjunction with {@link Hidden} marks a {@link ServiceRequest} non-executable by the user. This means it cannot be called from the outside
	 * via command-line, but the requests can be evaluated internally if needed.
	 */
	public static final String USE_CASE_EXECUTION = "execution";

	private static final EntityType<Deprecated> DeprecatedType = com.braintribe.model.meta.data.prompt.Deprecated.T;

	private String launchScript = "launch-script";
	private ServiceDomains serviceDomains;
	
	@Configurable
	public void setLaunchScript(String launchScript) {
		this.launchScript = launchScript;
	}
	
	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}
	
	@Override
	public Neutral process(ServiceRequestContext context, Help request) {
		if (request.getType() == null)
			getRequestOverview(request);
		else
			getRequestDetails(context, request);

		return NEUTRAL;
	}
	
	private CommandsReflection commandsReflection(ServiceDomain serviceDomain) {
		ModelMdResolver mdResolver = serviceDomain.systemCmdResolver().getMetaData().useCases(USE_CASE_HELP, USE_CASE_EXECUTION);
		CommandsReflection cr = new CommandsReflection(serviceDomain.modelOracle(), mdResolver);
		return cr;
	}

	private void getRequestDetails(ServiceRequestContext context, Help request) {
		
		GmEntityType type = null;
		CommandsReflection cr = null;
		
		for (ServiceDomain serviceDomain: serviceDomains.list()) {
			cr = commandsReflection(serviceDomain);

			CommandsOverview overview = cr.getCommandsOverview();

			type = cr.resolveTypeFromCommandName(request.getType());
			
			if (type == null) {
				EntityTypeOracle requestTypeOracle = cr.modelOracle.findEntityTypeOracle(request.getType());

				if (requestTypeOracle != null)
					type = requestTypeOracle.asGmEntityType();
			}
			
			if (type != null)
				break;
		}
		
		
		if (type == null)
			throw new IllegalArgumentException("unknown type: " + request.getType());


		println(sequence(
				// yellow("command line options:\n"),
				format(context, request, type, false, cr)));

	}

	private ConsoleOutput format(ServiceRequestContext context, Help request, GmEntityType requestType, boolean propertiesOnly, CommandsReflection cr) {
		ConfigurableConsoleOutputContainer sequence = configurableSequence();

		EntityMdResolver requestTypeMdResolver = cr.mdResolver.entityType(requestType);

		int lineWidth = resolveLineWidth(context);

		if (!propertiesOnly) {
			String typeSignature = requestType.getTypeSignature();
			String shortName = typeSignature.substring(typeSignature.lastIndexOf('.') + 1);
			String shortcut = StringTools.splitCamelCase(shortName).stream().map(String::toLowerCase).collect(Collectors.joining("-"));

			sequence.append("qualified-type: ");
			sequence.append(cyan(requestType.getTypeSignature() + "\n"));
			sequence.append("aliases: ");
			List<Alias> typeAliases = requestTypeMdResolver.meta(Alias.T).list();
			sequence.append(
					cyan(Stream.concat(Stream.of(shortcut), typeAliases.stream().map(Alias::getName)).collect(Collectors.joining(" ")) + "\n"));
			sequence.append("syntax: ");
			sequence.append(launchScript + " ");
			sequence.append(cyan(shortcut));

			PositionalArguments positionalArguments = requestTypeMdResolver.meta(PositionalArguments.T).exclusive();
			List<String> names = positionalArguments != null ? positionalArguments.getProperties() : Collections.emptyList();
			sequence.append(formatPositionalArguments(names));
			sequence.append(" [");
			sequence.append(cyan("--property "));
			sequence.append("<");
			sequence.append(brightBlack("value"));
			sequence.append(">]...\n");

			if (requestTypeMdResolver.is(DeprecatedType)) {
				sequence.append("\n");
				// TODO check with instanceof for ServiceRequest and use term "command" otherwise use term "type"
				sequence.append(yellow("This command is deprecated!"));
				sequence.append("\n");
			}

			String description = getDescriptionText(requestTypeMdResolver.meta(Description.T).exclusive());

			if (description != null) {
				sequence.append("\n");
				StringBuilder builder = new StringBuilder();
				format(builder, description, "", lineWidth);
				sequence.append(builder);
			}
		}

		List<GmProperty> relevantProperties = cr.getRelevantPropertiesOf(requestType);

		if (!relevantProperties.isEmpty()) {
			// if (!propertiesOnly)
			// sequence.append(yellow("\n arguments:\n"));

			EntityType<?> requestReflectionType = requestType.reflectionType();

			ConfigurableConsoleOutputContainer mandatoryOut = configurableSequence();
			ConfigurableConsoleOutputContainer optionalOut = configurableSequence();
			ConfigurableConsoleOutputContainer deprecatedOut = configurableSequence();
			final int colSize = 25;

			for (GmProperty property : relevantProperties) {
				PropertyMdResolver propertyMdResolver = requestTypeMdResolver.property(property);

				String propertyDescription = getDescriptionText(propertyMdResolver.meta(Description.T).exclusive());
				boolean mandatory = propertyMdResolver.is(Mandatory.T);
				boolean deprecated = propertyMdResolver.is(DeprecatedType);
				Property reflectionProperty = requestReflectionType.getProperty(property.getName());

				Object defaultValue = reflectionProperty.getDefaultValue();
				if (defaultValue instanceof EnumReference)
					defaultValue = ((EnumReference) defaultValue).getConstant();

				boolean effectiveMandatory = mandatory && defaultValue == null;

				final ConfigurableConsoleOutputContainer propertyBuilder;

				if (effectiveMandatory)
					propertyBuilder = mandatoryOut;
				else if (deprecated)
					propertyBuilder = deprecatedOut;
				else
					propertyBuilder = optionalOut;

				propertyBuilder.append("\n");

				String names = cr.cliNameAndAliasesOf(property) //
						.collect(Collectors.joining(" "));

				propertyBuilder.append(cyan(names));

				GenericModelType type = reflectionProperty.getType();

				final String argumentFragment;

				switch (type.getTypeCode()) {
					case mapType:
						argumentFragment = " KEY VAL ... ";
						break;

					case listType:
					case setType:
						argumentFragment = " ARG ... ";
						break;

					case booleanType:
						argumentFragment = " [ARG] ";
						break;

					default:
						argumentFragment = " ARG ";
						break;
				}

				propertyBuilder.append(brightBlack(argumentFragment));

				// propertyBuilder.append(" <");
				// propertyBuilder.append(brightBlack(propTypeSignature));
				// propertyBuilder.append(">");

				int parameterDefLength = names.length() + argumentFragment.length();

				int padding = Math.max(colSize - parameterDefLength, 0);
				propertyBuilder.append(padding(padding));
				propertyBuilder.append(": ");

				// if (parameterDefLength > colSize) {
				// propertyBuilder.append("\n");
				// propertyBuilder.append(pad);
				// }

				int firstLineConsumed = parameterDefLength + padding + 2;

				BlockFormatter formatter = new BlockFormatter() //
						.setIndent(colSize + 2) //
						.setWidth(lineWidth) //
						.setFirstLineConsumed(firstLineConsumed);

				if (deprecated)
					propertyBuilder.append(brightBlack(formatter.writeOutput("deprecated")));

				if (propertyDescription != null) {
					StringBuilder builder = new StringBuilder();

					formatter.format(builder, propertyDescription);
					propertyBuilder.append(builder);
				}

				// type
				// list element type
				// set element type
				// map key type
				// map value type

				switch (type.getTypeCode()) {
					case listType:
						propertyBuilder.append(brightBlack(
								formatter.writeOutput("list ARG type: " + formatType(((LinearCollectionType) type).getCollectionElementType()))));
						break;
					case setType:
						propertyBuilder.append(brightBlack(
								formatter.writeOutput("set ARG type: " + formatType(((LinearCollectionType) type).getCollectionElementType()))));
						break;

					case mapType:
						MapType mapType = (MapType) type;
						propertyBuilder.append(brightBlack(formatter.writeOutput("map KEY type: " + formatType(mapType.getKeyType()))));
						propertyBuilder.append(brightBlack(formatter.writeOutput("map VAL type: " + formatType(mapType.getValueType()))));
						break;

					default:
						propertyBuilder.append(brightBlack(formatter.writeOutput("ARG type: " + formatType(type))));
						break;
				}

				if (defaultValue != null) {
					/* we have to distinguish string and specifically empty strings and strings containing property placeholder values to make a
					 * rendering for these potentially irritating situations */

					String renderedDefault = null;

					if (defaultValue instanceof String) {
						String s = (String) defaultValue;

						if (s.isEmpty())
							renderedDefault = "default is an empty string";
						else if (containsPropertyPlaceholders(s))
							renderedDefault = "evaluable default: " + defaultValue.toString();
					}

					if (renderedDefault == null)
						renderedDefault = "default: " + defaultValue.toString();

					propertyBuilder.append(brightBlack(formatter.writeOutput(renderedDefault)));
				}

			}

			boolean includeDeprecated = request.getDeprecated();
			boolean includeUpToDate = request.getUpToDate();

			final boolean includeMandatory = request.getMandatory();
			final boolean includeOptional = request.getOptional();

			if (includeUpToDate) {
				if (includeMandatory)
					outputSectionIfNotEmpty(sequence, mandatoryOut, "mandatory properties");

				if (includeOptional)
					outputSectionIfNotEmpty(sequence, optionalOut, "optional properties");
			}

			if (includeDeprecated)
				outputSectionIfNotEmpty(sequence, deprecatedOut, "deprecated properties");
		}

		return sequence;
	}

	private int resolveLineWidth(ServiceRequestContext context) {
		return 160;
	}

	private static boolean containsPropertyPlaceholders(String s) {
		return s.contains("${");
	}

	private static String formatType(GenericModelType type) {
		Stream<String> values = null;
		switch (type.getTypeCode()) {
			case booleanType:
				values = Stream.of("true", "false");
				break;
			case enumType:
				values = Stream.of(((EnumType) type).getEnumValues()).map(Enum::name);
				break;
			default:
				break;
		}

		StringBuilder builder = new StringBuilder(type.getTypeSignature());

		if (values != null)
			builder.append(values.collect(Collectors.joining(", ", " (", ")")));

		return builder.toString();
	}

	private static void outputSectionIfNotEmpty(ConfigurableConsoleOutputContainer sequence, ConsoleOutputContainer output, String sectionTitle) {
		if (output.size() > 0) {
			sequence.append("\n" + sectionTitle + ":\n");
			sequence.append(output);
		}
	}

	private static String padding(int num) {
		char paddingChars[] = new char[num];
		Arrays.fill(paddingChars, ' ');
		return new String(paddingChars);
	}

	private static ConsoleOutput formatPositionalArguments(List<String> names) {
		ConfigurableConsoleOutputContainer builder = configurableSequence();

		for (String name : names) {
			builder.append(" [<");
			builder.append(cyan(name));
			builder.append(">");
		}

		for (int i = 0; i < names.size(); i++)
			builder.append("]");

		return builder;
	}
	
	private ConsoleOutput generateServiceDomainOverview(Help request, ServiceDomain serviceDomain) {
		ModelMdResolver mdResolver = serviceDomain.systemCmdResolver().getMetaData().useCases(USE_CASE_HELP, USE_CASE_EXECUTION);
		CommandsReflection cr = new CommandsReflection(serviceDomain.modelOracle(), mdResolver);
		CommandsOverview commandsOverview = cr.getCommandsOverview();
		
		// collect the dynamic part of the console output which consists of the requests
		ConfigurableConsoleOutputContainer commands = configurableSequence();
		ConfigurableConsoleOutputContainer deprecatedCommands = configurableSequence();

		boolean includeDeprecated = request.getDeprecated();
		boolean includeUpToDate = request.getUpToDate();

		for (GmEntityType type : commandsOverview.requestTypes) {
			String name = cr.resolveStandardAlias(type);

			boolean deprecated = mdResolver.entityType(type).is(DeprecatedType);

			if (deprecated)
				deprecatedCommands.append(cmdOutputLine(name));
			else
				commands.append(cmdOutputLine(name));
		}

		ConfigurableConsoleOutputContainer factoryCommands = configurableSequence();
		commandsOverview.inputTypes.stream() //
				.map(cr::resolveStandardAlias) //
				.map(this::cmdOutputLine) //
				.forEach(factoryCommands::append);

		// @formatter:off
		ConfigurableConsoleOutputContainer overview = configurableSequence();
		
		// @formatter:on

		if (commands.size() > 0 && includeUpToDate) {
			overview.append(text("\n\n"));
			overview.append(yellow(serviceDomain.domainId() + " commands:\n"));
			overview.append(commands);
		}

		if (deprecatedCommands.size() > 0 && includeDeprecated) {
			overview.append(text("\n\n"));
			overview.append(yellow(serviceDomain.domainId() + " deprecated commands:\n"));
			overview.append(deprecatedCommands);
		}
		
		return overview;
	}

	private void getRequestOverview(Help request) {

		boolean includeUpToDate = request.getUpToDate();
		
		CommandsReflection cr = commandsReflection(serviceDomains.byId("cli"));
		CommandsOverview commandsOverview = cr.getCommandsOverview();

		ConfigurableConsoleOutputContainer factoryCommands = configurableSequence();
		commandsOverview.inputTypes.stream() //
				.map(cr::resolveStandardAlias) //
				.map(this::cmdOutputLine) //
				.forEach(factoryCommands::append);

		// @formatter:off
		ConfigurableConsoleOutputContainer overview = configurableSequence().append(
			sequence(
				yellow("\nusage example: "), brightWhite(launchScript + " "), cyan("<command> "), text("--property "), cyan("value "),
					text("... "), text("[ : "), cyan("options "), text("--property "), cyan("value "), text("...] \n"),
					
				yellow("get help for a type (e.g. command): "),
				brightWhite(launchScript + " "),
				cyan("help "),
				cyan("<type>\n"),
				yellow("get help for options: "),
				brightWhite(launchScript + " "),
				cyan("help "),
				cyan("options\n")
			)
		);
		// @formatter:on

		if (factoryCommands.size() > 0 && includeUpToDate) {
			overview.append(text("\n\n"));
			overview.append(yellow("factory commands:\n"));
			overview.append(factoryCommands);
		}

		for (ServiceDomain serviceDomain: serviceDomains.list()) {
			overview.append(generateServiceDomainOverview(request, serviceDomain));
		}
		
		println(overview);
	}

	private String cmdOutputLine(String c) {
		return "\n    " + c;
	}

	private static void format(StringBuilder builder, String description, String indent, int width) {
		new BlockFormatter() //
				.setIndent(indent) //
				.setWidth(width) //
				.format(builder, description);
	}

	private static String getDescriptionText(Description description) {
		if (description == null)
			return null;

		LocalizedString ls = description.getDescription();
		return ls == null ? null : ls.value();
	}

}
