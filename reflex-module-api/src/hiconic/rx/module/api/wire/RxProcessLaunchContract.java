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
package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.space.WireSpace;

/**
 * Wire contract that exposes informations about the way the reflex platform process was launched
 * 
 * @author dirk.scheffler
 *
 */
public interface RxProcessLaunchContract extends WireSpace {
	
	/**
	 * The arguments that were passed to the platform's main method
	 */
	String[] cliArguments();
	
	/**
	 * The name of the launch script (bat/sh) that started the process
	 */
	String launchScriptName();

}
