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
package hiconic.rx.explorer.home.model;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Link extends GroupElement {

	final EntityType<Link> T = EntityTypes.T(Link.class);

	void setUrl(String url);
	String getUrl();

	void setDisplayName(String displayName);
	String getDisplayName();

	void setTarget(String target);
	String getTarget();

	void setType(String type);
	String getType();

	void setIconRef(String iconRef);
	String getIconRef();

	void setToolTip(String toolTip);
	String getToolTip();

}
