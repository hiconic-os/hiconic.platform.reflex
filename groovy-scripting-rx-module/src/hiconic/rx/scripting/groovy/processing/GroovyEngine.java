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
package hiconic.rx.scripting.groovy.processing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.resource.Resource;

import hiconic.rx.script.reason.ScriptCompileError;
import hiconic.rx.script.reason.ScriptRuntimeError;
import hiconic.rx.script.source.groovy.GroovyScript;
import hiconic.rx.scripting.api.CompiledScript;
import hiconic.rx.scripting.api.ScriptingEngine;

/**
 * Groovy {@link ScriptingEngine} implementation.
 */
public class GroovyEngine implements ScriptingEngine<GroovyScript> {

	private static final Compilable groovyEngine = (Compilable) new ScriptEngineManager().getEngineByName("Groovy");

	/**
	 * Compiles {@link GroovyScript} script into a {@link CompiledScript}.
	 * <p>
	 * May fail with {@link ScriptCompileError} reason.
	 * 
	 * @param script
	 *            A GroovyScript to be compiled.
	 * 
	 * @return Maybe<CompiledScript> Which may return the compiled script, or returns a reason for not doing so.
	 */
	@Override
	public Maybe<CompiledScript> compile(GroovyScript script) {
		return getResource(script).flatMap(r -> compileResource(r, script));
	}

	private Maybe<CompiledScript> compileResource(Resource resource, GroovyScript script) {
		try (Reader reader = new BufferedReader(new InputStreamReader(resource.openStream(), "UTF-8"))) {

			return Maybe.complete(new GroovyCompiledScript(script, groovyEngine.compile(reader)));

		} catch (ScriptException e) {
			return Reasons.build(ScriptCompileError.T) //
					.text("Error while compiling Groovy Script: " + script) //
					.cause(InternalError.from(e)) //
					.toMaybe();

		} catch (Exception e) {
			throw Exceptions.unchecked(e);
		}
	}

	private Maybe<Resource> getResource(GroovyScript script) {
		if (script == null)
			return Reasons.build(InvalidArgument.T).text("Missing script").toMaybe();

		Resource source = script.getSource();
		if (source == null)
			return Reasons.build(InvalidArgument.T).text("Missing source for script : " + script).toMaybe();
		return Maybe.complete(source);
	}

	// ############################################
	// ## . . . . . . CompiledScript . . . . . . ##
	// ############################################

	private static class GroovyCompiledScript implements CompiledScript {
		private final GroovyScript script;
		private final javax.script.CompiledScript compiledScript;

		public GroovyCompiledScript(GroovyScript script, javax.script.CompiledScript compiledScript) {
			this.script = script;
			this.compiledScript = compiledScript;
		}

		/**
		 * Evaluates Groovy {@link CompiledScript} given parameter bindings in context.
		 * <p>
		 * May fail with {@link ScriptRuntimeError}.
		 * 
		 * @param context
		 *            The parameter map passed on to the script.
		 * @return Maybe<T> Where the return type T depends on the script to be evaluated.
		 */
		@Override
		public <T> Maybe<T> evaluate(Map<String, Object> context) {
			Bindings bindings = new SimpleBindings();
			bindings.putAll(context);

			try {
				return Maybe.complete((T) compiledScript.eval(bindings));

			} catch (ScriptException e) {
				Throwable cause = e.getCause();
				if (cause instanceof UnsatisfiedMaybeTunneling)
					return ((UnsatisfiedMaybeTunneling) cause).getMaybe();

				return Reasons.build(ScriptRuntimeError.T) //
						.text("Error while evaluating Groovy Script: " + script) //
						.cause(InternalError.from(e)) //
						.toMaybe();
			}
		}
	}
}