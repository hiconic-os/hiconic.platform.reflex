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

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface StaticFilesystemResourceMapping extends GenericEntity {
	EntityType<StaticFilesystemResourceMapping> T = EntityTypes.T(StaticFilesystemResourceMapping.class);
	
	String path = "path";
	String rootDir = "rootDir";
	String welcomeFiles = "welcomeFiles";
	
	@Mandatory
	String getPath();
	void setPath(String path);
	
	@Mandatory
	String getRootDir();
	void setRootDir(String rootDir);

	List<String> getWelcomeFiles();
	void setWelcomeFiles(List<String> welcomeFiles);

}
