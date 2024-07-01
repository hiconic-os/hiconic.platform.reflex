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
package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface From extends GenericEntity {
	EntityType<From> T = EntityTypes.T(From.class);

	@Description("The mimetype defining the serialization format for the file input. "
			+ "Possible values: text/yaml, application/x-yaml, text/xml, application/json, gm/bin, gm/jse, gm/man")
	@Alias("m")
	@Initializer("'application/yaml'")
	@Mandatory
	String getMimeType();
	void setMimeType(String mimeType);
	
	@Description("Entities are created without initialized property default values to exactly reproduce a specific entity.")
	@Alias("r")
	boolean getReproduce();
	void setReproduce(boolean reproduce);
}
