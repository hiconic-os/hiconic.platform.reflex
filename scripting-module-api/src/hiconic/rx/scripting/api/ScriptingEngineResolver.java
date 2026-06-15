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
import com.braintribe.model.generic.reflection.EntityType;

import tribefire.extension.scripting.model.deployment.Script;

/**
 * Resolver of {@link ScriptingEngine}s for given {@link Script} types, with convenience methods to compile and evaluate a script.
 *
 * @author Dirk Scheffler
 */
public interface ScriptingEngineResolver {

	/**
	 * Resolves {@link ScriptingEngine} for given {@link Script} type.
	 * 
	 * @param <S>
	 *            {@link Script} type
	 * @param scriptType
	 *            {@link EntityType} for the {@link Script} type
	 * 
	 * @return Reason containing a {@link ScriptingEngine} for {@link Script} entity type.
	 */
	<S extends Script> Maybe<ScriptingEngine<S>> resolveEngine(EntityType<S> scriptType);

	/**
	 * Shortcut to evaluate a {@link Script}, chaining #{@link ScriptingEngineResolver} and {@link ScriptingEngine#evaluate(Script, Map)}.
	 * 
	 * @param <S>
	 *            {@link Script} type
	 * @param <T>
	 *            return type of given script
	 * @param script
	 *            the actual script
	 * @param bindings
	 *            input parameters passed to the script
	 * 
	 * @return Reason containing the script return object.
	 */
	default <S extends Script, T> Maybe<T> evaluate(S script, Map<String, Object> bindings) {
		return resolveEngine(script.entityType()).flatMap(engine -> engine.evaluate(script, bindings));
	}

	/**
	 * Shortcut to compiles a {@link Script} into a {@link CompiledScript}, chaining #{@link ScriptingEngineResolver} and
	 * {@link ScriptingEngine#compile(Script)}.
	 * 
	 * @param <S>
	 *            {@link Script} type
	 * @param script
	 *            the actual script
	 * @return Reason containing the {@link CompiledScript}.
	 */
	default <S extends Script> Maybe<CompiledScript> compile(S script) {
		return resolveEngine(script.entityType()).flatMap(engine -> engine.compile(script));
	}

}
