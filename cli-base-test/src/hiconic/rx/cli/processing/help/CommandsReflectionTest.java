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

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;
import static com.braintribe.utils.lcd.CollectionTools2.asList;
import static com.braintribe.utils.lcd.CollectionTools2.first;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.GmProperty;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.util.meta.NewMetaModelGeneration;
import com.braintribe.utils.lcd.CollectionTools2;

import hiconic.rx.cli.processing.help.CommandsReflection.CommandsOverview;
import hiconic.rx.cli.processing.help.model.ComRef_SimpleRequest;

/**
 * Tests for {@link CommandsReflection}
 * 
 * @author peter.gazdik
 */
public class CommandsReflectionTest {

	private final NewMetaModelGeneration mmg = new NewMetaModelGeneration(asList( //
			NewMetaModelGeneration.rootModel(), //
			ServiceRequest.T.getModel() //
	));

	// @formatter:off
	public static final Set<EntityType<?>> types =  CollectionTools2.<EntityType<?>>asSet (
			ComRef_SimpleRequest.T
	);
	// @formatter:on

	@Test
	public void testCommandsReflection() throws Exception {
		CommandsReflection ref = getComRef();

		CommandsOverview commandsOverview = ref.getCommandsOverview();
		assertThat(commandsOverview.requestTypes).hasSize(1);

		// Check request types
		GmEntityType reqType = first(commandsOverview.requestTypes);
		assertThat(reqType.getTypeSignature()).isEqualTo(ComRef_SimpleRequest.T.getTypeSignature());

		// Check relevant types
		List<GmProperty> relevantProps = ref.getRelevantPropertiesOf(reqType);
		Set<String> relevantPropNames = relevantProps.stream().map(GmProperty::getName).collect(Collectors.toSet());
		assertThat(relevantPropNames).containsExactly("name");
	}

	private CommandsReflection getComRef() {
		GmMetaModel gmModel = mmg.buildMetaModel( //
				"dev.hiconic.test:commands-reflection-test-model", //
				types, asList(ServiceRequest.T.getModel().getMetaModel()));

		ModelOracle modelOracle = new BasicModelOracle(gmModel);
		CmdResolver cmdResolver = CmdResolverImpl.create(modelOracle).done();

		return new CommandsReflection(modelOracle, cmdResolver.getMetaData());
	}

}
