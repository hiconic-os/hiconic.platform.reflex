// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.log.reflection.model.configuration;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogReflectionConfiguration extends GenericEntity {
	EntityType<LogReflectionConfiguration> T = EntityTypes.T(LogReflectionConfiguration.class);

	@Initializer("{'log-reflection-reader','log-reflection-manager'}")
	Set<String> getReaderRoles();
	void setReaderRoles(Set<String> readerRoles);

	@Initializer("{'log-reflection-manager'}")
	Set<String> getManagerRoles();
	void setManagerRoles(Set<String> managerRoles);
}
