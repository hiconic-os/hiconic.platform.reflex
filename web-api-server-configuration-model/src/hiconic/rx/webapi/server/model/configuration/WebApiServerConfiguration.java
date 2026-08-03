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
package hiconic.rx.webapi.server.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebApiServerConfiguration extends GenericEntity {

	EntityType<WebApiServerConfiguration> T = EntityTypes.T(WebApiServerConfiguration.class);

	String endpointPath = "endpointPath";
	String defaultResponseDepth = "defaultResponseDepth";

	@Description("Path at which the Web API transport is mounted, relative to the web server's default endpoints base path. "
			+ "The value must not be empty and must not start or end with '/'.")
	@Initializer("'api'")
	String getEndpointPath();
	void setEndpointPath(String endpointPath);

	@Description("Default response traversal depth used when an individual Web API mapping does not define one.")
	@Initializer("'reachable'")
	String getDefaultResponseDepth();
	void setDefaultResponseDepth(String defaultResponseDepth);
}
