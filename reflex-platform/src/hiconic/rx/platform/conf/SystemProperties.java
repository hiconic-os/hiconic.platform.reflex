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
package hiconic.rx.platform.conf;

import java.io.File;

import com.braintribe.wire.api.annotation.Default;
import com.braintribe.wire.api.annotation.Name;

public interface SystemProperties {
	static String PROPERTY_APP_DIR = "reflex.app.dir";
	static String PROPERTY_LAUNCH_SCRIPT = "reflex.launch.script"; 
	
	@Name(PROPERTY_APP_DIR)
	@Default(".")
	File appDir();
	
	@Name(PROPERTY_LAUNCH_SCRIPT)
	@Default("launch-script")
	String launchScript();
}
