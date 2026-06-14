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

/**
 * Interface for compiled scripts that can be evaluated. Scripting engines internally will pre-compile scripts and store the result as a
 * CompiledScript.
 */
public interface CompiledScript {

	/**
	 * To evaluate a compiled script given parameter bindings. The script is already pre-compiled and the bindings are passed as parameter inputs to
	 * the script.
	 * 
	 * @param <T>
	 *            Arbitrary return type, depending on the actual script.
	 * @param bindings
	 *            Parameter bindings. The parameters are passed to the script as inputs.
	 * 
	 * @return Reasoned return object. May contain ScriptRuntimeError.
	 */
	<T> Maybe<T> evaluate(Map<String, Object> bindings);

}
