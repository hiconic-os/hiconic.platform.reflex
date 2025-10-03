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
package hiconic.platform.reflex.web_server.processing.exception;

import java.util.function.Function;

/**
 * Interface for code that deals with general exceptions in the {@link ExceptionFilter}.
 * <p><p>
 * The interface extends a function that takes a {@link ExceptionHandlingContext} object and return
 * a boolean value. True means, that the exception has been dealt with, false if nothing happened.
 * If none of the configured handlers is able to return true, a default mechnanism takes place.
 * Returning true does not prevent other handlers from processing the exception.
 * 
 * @author roman.kurmanowytsch
 */
public interface ExceptionHandler extends Function<ExceptionHandlingContext,Boolean> {

	//Intentionally left empty
	
}
