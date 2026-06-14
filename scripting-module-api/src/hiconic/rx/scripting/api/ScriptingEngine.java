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
package hiconic.rx.scripting.api;

import java.util.Map;

import com.braintribe.gm.model.reason.Maybe;

import hiconic.rx.script.reason.ScriptCompileError;
import hiconic.rx.script.reason.ScriptRuntimeError;
import hiconic.rx.script.source.Script;

/**
 * Primarily a compiler turning given {@link Script} into an evaluable {@link CompiledScript}, with a convenience method to evaluate a script in one
 * step.
 * 
 * @author Dirk Scheffler
 *
 * @param <S>
 *            Denotes a concrete {@link Script} type.
 */
public interface ScriptingEngine<S extends Script> {

	/**
	 * To evaluate a script given the parameters in bindings. The method may return a {@link ScriptRuntimeError}.
	 * 
	 * @param <T>
	 *            Arbitrary return type, depending on the actual script to be executed.
	 * @param script
	 *            Script data.
	 * @param bindings
	 *            Parameter bindings as map, which are passed as inputs to the script.
	 * 
	 * @return A Reasoned return object, with a type that depends on the actual script object.
	 */
	default <T> Maybe<T> evaluate(S script, Map<String, Object> bindings) {
		return compile(script).flatMap(compiledScript -> compiledScript.evaluate(bindings));
	}

	/**
	 * To compile a script. The method may return a {@link ScriptCompileError}.
	 * 
	 * @param script
	 *            Script data.
	 * 
	 * @return A reasoned {@link CompiledScript} object.
	 */
	Maybe<CompiledScript> compile(S script);
}
