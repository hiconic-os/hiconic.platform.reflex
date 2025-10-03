// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.reflection.model.system.os;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Locale extends GenericEntity {

	EntityType<Locale> T = EntityTypes.T(Locale.class);

	String getName();
	void setName(String name);

	String getDisplayName();
	void setDisplayName(String displayName);

	String getCountry();
	void setCountry(String country);

	String getDisplayCountry();
	void setDisplayCountry(String displayCountry);

	String getLanguage();
	void setLanguage(String language);

	String getDisplayLanguage();
	void setDisplayLanguage(String displayLanguage);

}
