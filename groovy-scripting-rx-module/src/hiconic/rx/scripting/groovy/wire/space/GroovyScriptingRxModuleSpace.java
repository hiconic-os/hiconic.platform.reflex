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
package hiconic.rx.scripting.groovy.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.script.source.groovy.GroovyScript;
import hiconic.rx.scripting.api.ScriptingConfigurationContract;
import hiconic.rx.scripting.groovy.processing.GroovyEngine;

/**
 * Registers {@link GroovyEngine} as the engine for {@link GroovyScript}.
 */
@Managed
public class GroovyScriptingRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private ScriptingConfigurationContract scriptingConfiguration;

	@Override
	public void configurePlatform(RxPlatformConfigurator ignored) {
		scriptingConfiguration.registerScriptingEngine(GroovyScript.T, groovyEngine());
	}

	@Managed
	private GroovyEngine groovyEngine() {
		GroovyEngine bean = new GroovyEngine();

		return bean;
	}

}