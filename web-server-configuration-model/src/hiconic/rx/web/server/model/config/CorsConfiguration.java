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
package hiconic.rx.web.server.model.config;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface CorsConfiguration extends GenericEntity {

	EntityType<CorsConfiguration> T = EntityTypes.T(CorsConfiguration.class);

	boolean getAllowAnyOrigin();
	void setAllowAnyOrigin(boolean allowAnyOrigin);

	Set<String> getAllowedOrigins();
	void setAllowedOrigins(Set<String> allowedOrigins);

	int getMaxAge();
	void setMaxAge(int maxAge);

	Set<String> getSupportedMethods();
	void setSupportedMethods(Set<String> supportedMethods);

	boolean getSupportAnyHeader();
	void setSupportAnyHeader(boolean supportAnyHeader);

	Set<String> getSupportedHeaders();
	void setSupportedHeaders(Set<String> supportedHeaders);

	Set<String> getExposedHeaders();
	void setExposedHeaders(Set<String> exposedHeaders);

	boolean getSupportsCredentials();
	void setSupportsCredentials(boolean supportsCredentials);
}
