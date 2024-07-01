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
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Creates a Reflex project consisting of a model, module and an application that bundles it all together with additional modules.")
public interface CreateReflexProject extends CreateArtifact {

	EntityType<CreateReflexProject> T = EntityTypes.T(CreateReflexProject.class);

	@Alias("e")
	@Description("Specifies the intended endpoint. The only effect is that a corresponding module dependency will be added to the application.")
	@Initializer("web")
	RxEndpoint getEndpoint();
	void setEndpoint(RxEndpoint endpoint);

	@Override
	default String template() {
		return ReflexTemplateLocator.template("reflex-project-template");
	}

	@Override
	default boolean delegatingOnly() {
		return true;
	}
}
