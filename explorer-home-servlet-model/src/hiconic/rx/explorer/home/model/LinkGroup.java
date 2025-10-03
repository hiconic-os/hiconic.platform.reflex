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

import java.util.List;

import com.braintribe.model.generic.StandardIdentifiable;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LinkGroup extends StandardIdentifiable {

	final EntityType<LinkGroup> T = EntityTypes.T(LinkGroup.class);

	void setName(String name);
	String getName();

	void setIconRef(String iconRef);
	String getIconRef();

	void setOpenLink(Link openLink);
	Link getOpenLink();
	
	void setLinks(List<GroupElement> links);
	List<GroupElement> getLinks();
	
	boolean getSkip();
	void setSkip(boolean skip);

}
