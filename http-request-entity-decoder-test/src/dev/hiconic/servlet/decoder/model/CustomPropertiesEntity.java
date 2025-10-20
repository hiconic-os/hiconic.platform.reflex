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
package dev.hiconic.servlet.decoder.model;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * This entity contains only custom properties (no standard header properties).
 * 
 *
 */
public interface CustomPropertiesEntity extends GenericEntity {
	
	static EntityType<CustomPropertiesEntity> T = EntityTypes.T(CustomPropertiesEntity.class);
	
	// shared with MixedPropertiesEntity
	
	String getStringProperty();
	void setStringProperty(String stringProperty);

	boolean getBooleanProperty();
	void setBooleanProperty(boolean booleanProperty);

	int getIntProperty();
	void setIntProperty(int intProperty);

	// Own properties
	
	Date getDateProperty();
	void setDateProperty(Date dateProperty);
	
	TestEnumeration getEnumProperty();
	void setEnumProperty(TestEnumeration enumProperty);
}
