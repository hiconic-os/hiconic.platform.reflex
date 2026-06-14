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
package hiconic.rx.scripting.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.core.expert.api.MutableDenotationMap;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;

import hiconic.rx.script.source.Script;
import hiconic.rx.scripting.api.ScriptingEngine;
import hiconic.rx.scripting.api.ScriptingEngineResolver;

/**
 * @author peter.gazdik
 */
public class ScriptingEngineRegistry implements ScriptingEngineResolver {

	private final MutableDenotationMap<Script, ScriptingEngine<?>> engines = new PolymorphicDenotationMap<>();

	public <S extends Script> void registerScriptingEngine(EntityType<S> scriptType, ScriptingEngine<S> engine) {
		engines.put(scriptType, engine);
	}

	@Override
	public <S extends Script> Maybe<ScriptingEngine<S>> resolveEngine(EntityType<S> scriptType) {
		ScriptingEngine<S> engine = engines.get(scriptType);
		if (engine != null)
			return Maybe.complete(engine);
		else
			return NotFound.create("ScriptEngine not found for script type: " + scriptType.getTypeSignature()).asMaybe();
	}

}
