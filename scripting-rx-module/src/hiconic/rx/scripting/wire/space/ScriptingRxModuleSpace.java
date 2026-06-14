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
package hiconic.rx.scripting.wire.space;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.script.source.Script;
import hiconic.rx.scripting.api.ScriptingConfigurationContract;
import hiconic.rx.scripting.api.ScriptingContract;
import hiconic.rx.scripting.api.ScriptingEngine;
import hiconic.rx.scripting.processing.ScriptingEngineRegistry;

/**
 * Support for {@link ScriptingContract} and {@link ScriptingConfigurationContract}.
 * <p>
 * IMPLEMENTATION: Straight forward internal registry for {@link ScriptingEngine}s, mapping from their corresponding {@link Script} types via a
 * {@link PolymorphicDenotationMap}.
 */
@Managed
public class ScriptingRxModuleSpace implements RxModuleContract, ScriptingContract, ScriptingConfigurationContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public <S extends Script> void registerScriptingEngine(EntityType<S> scriptType, ScriptingEngine<S> engine) {
		scriptingEngineResolver().registerScriptingEngine(scriptType, engine);
	}

	@Override
	@Managed
	public ScriptingEngineRegistry scriptingEngineResolver() {
		ScriptingEngineRegistry bean = new ScriptingEngineRegistry();

		return bean;
	}

}