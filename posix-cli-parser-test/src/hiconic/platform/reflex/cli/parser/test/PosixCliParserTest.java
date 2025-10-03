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
package hiconic.platform.reflex.cli.parser.test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.cli.posix.parser.PosixCommandLineParser;
import com.braintribe.gm.cli.posix.parser.api.ParsedCommandLine;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;

import hiconic.platform.reflex._PosixCliParserTestModel_;
import hiconic.platform.reflex.cli.parser.test.model.TakesAMap;
import hiconic.platform.reflex.cli.parser.test.model.TestEntity;
import hiconic.platform.reflex.cli.parser.test.model.TestEnum;
import hiconic.platform.reflex.cli.parser.test.processing.TestCliEntityEvaluator;

public class PosixCliParserTest {

	private static CmdResolver cmdResolver = buildCmdResolver();
	
	@Test
	public void testScalarTypeMismatch() {
		Maybe<TestEntity> maybe = parseReasoned(TestEntity.T, "test-entity", "-b", "truexyz");
		
		System.out.println(maybe.whyUnsatisfied().stringify());
		
	}
	
	@Test
	public void testScalars() {
		TestEntity e = parse(TestEntity.T, //
				"test-entity", //
				"-b", "true", //
				"-i", "5", //
				"-f", "2.18", //
				"-d", "3.14", //
				"-D", "1231414235143521445214442135142312", //
				"-e", "ONE", //
				"--stringValue", "Hello World"
		);
		
		Assertions.assertThat(e.getPrimitiveBooleanValue()).isEqualTo(true);
		Assertions.assertThat(e.getIntValue()).isEqualTo(5);
		Assertions.assertThat(e.getPrimitiveFloatValue()).isEqualTo(2.18f);
		Assertions.assertThat(e.getPrimitiveDoubleValue()).isEqualTo(3.14D);
		Assertions.assertThat(e.getDecimalValue()).isEqualTo(new BigDecimal("1231414235143521445214442135142312"));
		Assertions.assertThat(e.getEnumValue()).isEqualTo(TestEnum.ONE);
		Assertions.assertThat(e.getStringValue()).isEqualTo("Hello World");
	}
	
	private <E extends GenericEntity> E parse(EntityType<E> type, String... args) {
		return parseReasoned(type, args).get();
	}
	
	private <E extends GenericEntity> Maybe<E> parseReasoned(EntityType<E> type, String... args) {
		List<String> argList = Arrays.asList(args);
		
		PosixCommandLineParser parser = parser();
		
		Maybe<ParsedCommandLine> maybe = parser.parseReasoned(argList, Collections.singletonList("main"));
		
		if (maybe.isUnsatisfied())
			return maybe.whyUnsatisfied().asMaybe();
		
		ParsedCommandLine cl = maybe.get();
		Optional<E> optional = cl.findInstance(type);
		
		Assertions.assertThat(optional.isPresent()).isTrue();
		
		return Maybe.complete(optional.get());
	}
	
	@Test
	public void testMapFromFile() {
		List<String> args = List.of("takes-a-map", "-m", "@map", ":map", "parse-url-query", "-q", "one=1&two=2&three=3");
		
		PosixCommandLineParser parser = parser();
		
		Maybe<ParsedCommandLine> maybe = parser.parseReasoned(args, Collections.singletonList("main"));
		

		ParsedCommandLine cl = maybe.get();
		Optional<TakesAMap> optional = cl.findInstance(TakesAMap.T);
		
		Assertions.assertThat(optional.isPresent()).isTrue();
		
		TakesAMap takesAMap = optional.get();
		
		Map<String, String> map = takesAMap.getMap();
		
		Map<String, String> expectedMap = Map.of("one", "1", "two", "2", "three", "3");
		
		Assertions.assertThat(map).isEqualTo(expectedMap);
	}
	
	@Test
	public void testMap() {
		List<String> args = List.of("takes-a-map", "-m", "one", "1", "two", "2", "three", "3");
		
		PosixCommandLineParser parser = parser();
		
		Maybe<ParsedCommandLine> maybe = parser.parseReasoned(args, Collections.singletonList("main"));
		
		ParsedCommandLine cl = maybe.get();
		Optional<TakesAMap> optional = cl.findInstance(TakesAMap.T);
		
		Assertions.assertThat(optional.isPresent()).isTrue();
		
		TakesAMap takesAMap = optional.get();
		
		Map<String, String> map = takesAMap.getMap();
		
		Map<String, String> expectedMap = Map.of("one", "1", "two", "2", "three", "3");
		
		Assertions.assertThat(map).isEqualTo(expectedMap);
	}
	
	private static PosixCommandLineParser parser() {
		PosixCommandLineParser parser = new PosixCommandLineParser(d -> d.equals("main")? cmdResolver: null);
		parser.setEntityEvaluator(new TestCliEntityEvaluator());
		return parser;
	}
	
	private static CmdResolver buildCmdResolver() {
		ModelOracle oracle = new BasicModelOracle(GMF.getTypeReflection().getModel(_PosixCliParserTestModel_.name).getMetaModel());
		return CmdResolverImpl.create(oracle).done();
	}
}
