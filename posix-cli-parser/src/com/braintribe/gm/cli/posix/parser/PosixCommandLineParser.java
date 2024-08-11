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
package com.braintribe.gm.cli.posix.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.braintribe.gm.cli.posix.parser.api.CliEntityEvaluator;
import com.braintribe.gm.cli.posix.parser.api.ParsedCommandLine;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.meta.cmd.CmdResolver;


public class PosixCommandLineParser extends AbstractCommandLineParser {
	private static final List<String> booleanValues = Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString());
	
	private Map<String, Maybe<CommandLineParserTypeIndex>> typeIndices = new ConcurrentHashMap<>();
	private Function<String, CmdResolver> cmdResolverProvider;
	private Function<EntityType<?>, GenericEntity> entityFactory = EntityType::create;
	
	public PosixCommandLineParser(Function<String, CmdResolver> cmdResolverProvider) {
		this.cmdResolverProvider = cmdResolverProvider;
	}
	
	public void setEntityFactory(Function<EntityType<?>, GenericEntity> entityFactory) {
		this.entityFactory = entityFactory;
	}
	
	private Maybe<CommandLineParserTypeIndex> findTypeIndex(String domainId) {
		return typeIndices.computeIfAbsent(domainId, this::_findTypeIndex);
	}
	
	private Maybe<CommandLineParserTypeIndex> _findTypeIndex(String domainId) {
		CmdResolver cmdResolver = cmdResolverProvider.apply(domainId);
		if (cmdResolver != null)
			return Maybe.complete(new CommandLineParserTypeIndex(cmdResolver));
		else {
			return Reasons.build(NotFound.T) //
					.text("Unkown domain " + domainId) //
					.toMaybe();			
		}
	}
	
	enum RawArgumentKind {
		propertyShorthands,
		propertyName,
		value,
	}
	
	private CliEntityEvaluator entityEvaluator = EmptyCliEntityEvaluator.INSTANCE;

	public void setEntityEvaluator(CliEntityEvaluator fromExperts) {
		this.entityEvaluator = fromExperts;
	}
	
	public Maybe<ParsedCommandLine> parseReasoned(List<String> args, List<String> defaultDomains) {
		ParsedCommandLineImpl parsedCommandLine = new ParsedCommandLineImpl();
		
		List<RawOptions> splitOptions = splitOptions(args);
		
		Map<String, Object> variables = new HashMap<>();
		
		for (RawOptions rawOption: splitOptions) {
			if (rawOption.options.isEmpty())
				continue;
			
			Maybe<?> parsedMaybe = new StatefulParser(rawOption.index, rawOption.options, defaultDomains, variables).parseReasoned();
			
			if (parsedMaybe.isUnsatisfied())
				return parsedMaybe.whyUnsatisfied().asMaybe();

			Object parsed = parsedMaybe.get();
			
			
			//GenericEntity entity = parsedMaybe.get();

			if (rawOption.name != null) {
				variables.compute(rawOption.name, (k, v) -> {
					if (v != null) {
						if (v.getClass() == ForwardReference.class) {
							((ForwardReference)v).consumers.forEach(c -> c.accept(parsed));
						}
						else
							throw parseError("duplicate variable definition:" + k);
					}
					return parsed;
				});
			}
			else {
				if (parsed instanceof GenericEntity entity)
					parsedCommandLine.addEntity(entity);
				else
					return Reasons.build(InvalidArgument.T) //
							.text("Non entity values are only accepted for named sections values") //
							.toMaybe();
			}
		}
		
		return Maybe.complete(parsedCommandLine);
	}
	
	public ParsedCommandLine parse(List<String> args, List<String> defaultDomains) {
		return parseReasoned(args, defaultDomains).get();
	}
	
	
	private static RawArgumentKind getKind(String rawArg) {
		if (rawArg.startsWith("--"))
			return RawArgumentKind.propertyName;
		else if (rawArg.startsWith("-")) {
			return RawArgumentKind.propertyShorthands;
		}
		else {
			return RawArgumentKind.value;
		}
			
	}
	
	private static final Object noValue = new Object();
	
	private static class RawOptions {
		private String name;
		private int index;
		private List<String> options = new ArrayList<>();
	}

	private List<RawOptions> splitOptions(List<String> args) {
		List<RawOptions> optionsList = new ArrayList<>();
		
		RawOptions currentOptions = new RawOptions();
		optionsList.add(currentOptions);
		
		int index = 0;
		for (String arg: args) {
			if (arg.startsWith(":")) {
				currentOptions = new RawOptions();
				currentOptions.index = index + 1;
				if (arg.length() > 1) {
					currentOptions.name = arg.substring(1);
				}
				optionsList.add(currentOptions);
			}
			else {
				currentOptions.options.add(arg);
			}
			
			index++;
		}
		
		return optionsList;
	}

	
	private class StatefulParser implements CliArgumentParser {
		
		private GenericEntity options;
		private GenericEntity evaluable;
		
		private List<String> args;
		private CliArgument currentArg;
		private int argIndex = 0;
		private int argOffset;
		
		private Map<String, Object> variables;
		
		private String defaultDomainId;
		
		private CommandLineParserTypeIndex currentTypeIndex;
		private List<String> defaultDomains;
		
		StatefulParser(int argOffset, List<String> args, List<String> defaultDomains, Map<String, Object> variables) {
			this.argOffset = argOffset;
			this.args = args;
			this.defaultDomains = defaultDomains;
			this.variables = variables;
		}
		
		@Override
		public ParseException parseError(String msg) {
			String location = " at " + currentArg;
			return new ParseException(msg + location);
		}
		
		private String nextRawArg() {
			CliArgument argument = nextCliArgument();
			
			if (argument == null)
				return null;
			
			return argument.getInput();
		}
		
		private CliArgument nextCliArgument() {
			if (argIndex < args.size()) {
				String rawArg = args.get(argIndex++);
				currentArg = new CliArgument(argOffset + argIndex, rawArg);
				return currentArg;
			}
			else
				return null;
		}
		
		private void rewindOneRawArg() {
			argIndex--;
		}
		
		Maybe<Object> parseReasoned() {
			try {
				return Maybe.complete(parse());
			} catch (ParseException e) {
				return Reasons.build(ParseError.T).text(e.getMessage()).toMaybe();
			} catch (UnsatisfiedMaybeTunneling e) {
				return e.getMaybe();
			}
		}
		
		Object parse() {
			if (!parseInstantiation())
				return null;
			
			parseSequencedArguments();
			
			parseNamedArguments();
			
			if (evaluable != null && evaluable == options) {
				return entityEvaluator.evaluate(evaluable);
			}
			
			return options;
		}
		
		private boolean parseInstantiation() {
			String domainAndTypeIdentifier = nextRawArg();
			
			if (domainAndTypeIdentifier == null)
				return false;
			
			int indexOfSlash = domainAndTypeIdentifier.indexOf('/');
			
			final List<String> domainIds;
			final String typeIdentifier;
			
			if (indexOfSlash != -1) {
				typeIdentifier = domainAndTypeIdentifier.substring(indexOfSlash + 1);
				domainIds = Collections.singletonList(domainAndTypeIdentifier.substring(0, indexOfSlash));
			}
			else {
				typeIdentifier = domainAndTypeIdentifier;
				domainIds = defaultDomains;
			}

			options = createEntity(domainIds, typeIdentifier);
			
			
			if (entityEvaluator.isEvaluable(options)) {
				evaluable = options;
			}
			
			return true;
		}
		
		private GenericEntity createEntity(List<String> domainIds, String typeIdentifier) {
			for (String domainId: domainIds) {
				Maybe<CommandLineParserTypeIndex> indexMaybe = findTypeIndex(domainId);
				
				if (indexMaybe.isUnsatisfied()) {
					throw parseError("Unknown service domain [" + domainId + "]");
				}
				
				CommandLineParserTypeIndex index = indexMaybe.get();
				
				EntityType<?> entityType = index.findEntityType(typeIdentifier);
				
				if (entityType != null) {
					this.currentTypeIndex = index; 
					return entityFactory.apply(entityType);
				}
			}
			
			throw parseError("Unsupported type [" + typeIdentifier + "]");
		}
		
		private Consumer<Object> getAssigner(GenericEntity entity, Property property) {
			return v -> {
				if (v != null && v.getClass() == ForwardReference.class) {
					ForwardReference forwardReference = (ForwardReference)v;
					forwardReference.consumers.add(d -> property.set(entity, d));
				}
				else {
					property.set(entity, v);
				}
			};
		}
		
		private void parsePropertyShorthands(GenericEntity entity, String shorthands) {
			GenericModelType propertyType = null;
			Consumer<Object> consumer = null; 
			
			for (int i = 0; i < shorthands.length(); i++) {
				char s = shorthands.charAt(i);
				
				CommandLineParserTypeIndex oracle = getOracle();
				Property property = oracle.findProperty(entity.entityType(), String.valueOf(s));
				
				if (property == null)
					throw parseError(entity.entityType() + " has no property with name [" + s +"]");
				
				if (propertyType == null) {
					propertyType = property.getType();
					consumer = getAssigner(entity, property);
				}
				else {
					if (propertyType != property.getType())
						throw parseError("Single character arguments with different types cannot combined");
					
					consumer = consumer.andThen(getAssigner(entity, property));
				}
			}
			
			parseValues(consumer, propertyType);
		}
		
		private CommandLineParserTypeIndex getOracle() {
			return currentTypeIndex;
		}

		private void parseProperty(GenericEntity entity, String propertyName) {
			Property property = getOracle().findProperty(entity.entityType(), propertyName);
			
			if (property == null)
				throw parseError(entity.entityType() + " has no property [" + propertyName +"]");

			Consumer<Object> consumer = getAssigner(entity, property);
			parseValues(consumer, property.getType());
		}
		
		private void parseValues(Consumer<Object> consumer, GenericModelType inferredType) {
			switch (inferredType.getTypeCode()) {
				case booleanType:
					Object optionalValue = parseOptionalValue(EssentialTypes.TYPE_BOOLEAN, booleanValues);
					
					if (optionalValue != noValue)
						consumer.accept(optionalValue);
					else
						consumer.accept(Boolean.TRUE);
					return;
					
				case mapType: {
					new MapAccumulator(this, consumer, (MapType)inferredType).parse();
					return;
				}
				case listType:
				case setType:
					new LinearCollectionAccumulator(this, consumer, (LinearCollectionType)inferredType).parse();
					return;
					
				default:
					consumer.accept(parseValue(inferredType));
					return;
			}
		}
		
		@Override
		public CliArgument parseArgumentValue(GenericModelType inferredType) {
			CliArgument argument = nextCliArgument();
			
			if (argument == null)
				throw parseError("Unexpected end of arguments when reading an argument");
			
			Object value = parseValue(inferredType, argument.getInput());
			argument.setValue(value);
			return argument;
		}
		
		@Override
		public CliArgument parseOptionalArgumentValue(GenericModelType inferredType, List<String> positiveValues) {

			CliArgument argument = nextCliArgument();
			
			if (argument == null)
				return null;
			
			String input = argument.getInput();
			
			if (getKind(input) == RawArgumentKind.value) {
				if (positiveValues != null) {
					if (positiveValues.contains(input)) {
						Object value = parseValue(inferredType, input);
						argument.setValue(value);
						return argument;
					}
					else {
						rewindOneRawArg();
						return null;
					}
				}
				else {
					Object value = parseValue(inferredType, input);
					argument.setValue(value);
					return argument;
				}
			}
			else {
				rewindOneRawArg();
				return null;
			}
		}

		private Object parseOptionalValue(GenericModelType inferredType, List<String> positiveValues) {
			CliArgument argument = parseOptionalArgumentValue(inferredType, positiveValues);
			
			if (argument == null)
				return noValue;
			
			return argument.getValue();
		}
		
		private Object parseValue(GenericModelType inferredType) {
			CliArgument argument = parseArgumentValue(inferredType);
			return argument.getValue();
		}
		
		private Object parseValue(GenericModelType inferredType, String rawArg) {
			if (rawArg.startsWith("@")) {
				String name = rawArg.substring(1);
				if (name.startsWith("@"))
					rawArg = name;
				else
					return variables.computeIfAbsent(name, k -> new ForwardReference());
			}
			else if (rawArg.equals("null")) {
				return null;
			}
			
			if (inferredType == BaseType.INSTANCE) {
				String typeName = rawArg;
				return parseValue(GMF.getTypeReflection().getType(typeName));
			}
			else {
				return decodeValue(inferredType, PosixEscaping.unescape(rawArg));
			}
			
		}
		
		private boolean parseOptionalExtensionOperator() {
			if (evaluable == null)
				return false;
			
			String rawArg = nextRawArg();
			
			if (rawArg == null) {
				return false;
			}
			
			if (rawArg.equals("+")) {
				Object evaluated = entityEvaluator.evaluate(evaluable);
				
				if (!(evaluated instanceof GenericEntity entity))
					throw parseError("+ is only allowed on entities");
				
				options = entity;
				evaluable = null;
				parseNamedArguments();
				return true;
			}
			
			rewindOneRawArg();
			return false;
		}

		private void parseSequencedArguments() {
			List<Property> properties = getPropertySequence();
			
			for (Property property: properties) {
				if (property.getType().isCollection())
					continue;
				
				if (parseOptionalExtensionOperator())
					return;
				
				Object value = parseOptionalValue(property.getType(), null);
				
				if (value == noValue)
					break;
				
				property.set(options, value);
			}
		}
		
		private void parseNamedArguments() {
			while (parseNamedArgument()) { /*noop*/ }
		}
		
		private boolean parseNamedArgument() {
			if (parseOptionalExtensionOperator())
				return false;
			
			String rawArg = nextRawArg();
			
			if (rawArg == null)
				return false;
			
			switch(getKind(rawArg)) {
				case propertyName:
					parseProperty(options, rawArg.substring(2));
					return true;
				case propertyShorthands:
					parsePropertyShorthands(options, rawArg.substring(1));
					return true;
				default:
					throw parseError("Expected a named argument");
			}
		}


		private List<Property> getPropertySequence() {
			return getOracle().getPositionalArguments(options.entityType());
		}
		
	}
	
	private ParseException parseError(String msg) {
		return new ParseException(msg);
	}
}
