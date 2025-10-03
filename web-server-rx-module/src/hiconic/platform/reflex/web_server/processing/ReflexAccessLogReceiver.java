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
package hiconic.platform.reflex.web_server.processing;

import com.braintribe.logging.Logger;

public class ReflexAccessLogReceiver implements io.undertow.server.handlers.accesslog.AccessLogReceiver {
	private Logger logger = Logger.getLogger(ReflexAccessLogReceiver.class);
	@Override
	public void logMessage(String message) {
		//logger.info(message);
	}
}