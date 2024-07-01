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
package hiconic.rx.setup.model;

import com.braintribe.devrock.templates.model.artifact.CreateArtifact;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Creates a Reflex application.")
public interface CreateReflexApp extends CreateArtifact {

	EntityType<CreateReflexApp> T = EntityTypes.T(CreateReflexApp.class);

	@Alias("m")
	@Description("Specifies whether the app is also a module, or is just an aggregator of its dependencies.")
	boolean getIsModule();
	void setIsModule(boolean isModule);

	@Override
	default String template() {
		return ReflexTemplateLocator.template("reflex-app-template");
	}

}
