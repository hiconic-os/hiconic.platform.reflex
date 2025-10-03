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
package hiconic.platform.reflex.security.processor.orm;

import com.braintribe.gm.jdbc.api.GmColumnBuilder;
import com.braintribe.gm.jdbc.api.GmDb;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;

public class EntityMappings {
	public static GmColumnBuilder<?> builder(GmDb db, Property property) {
		GenericModelType propertyType = property.getType();
		
		return builder(db, propertyType, property.getName());
	}
	
	@SuppressWarnings("rawtypes")
	public static GmColumnBuilder<?> builder(GmDb db, GenericModelType type, String name) {
		switch (type.getTypeCode()) {
			// @formatter:off
			case booleanType: return db.booleanCol(name);
			case dateType: return db.date(name);
			case decimalType: return db.bigDecimal(name);
			case doubleType: return db.doubleCol(name);
			case enumType: return db.enumCol(name, (Class<Enum>) type.getJavaType());
			case floatType: return db.floatCol(name);
			case integerType: return db.intCol(name);
			case longType: return db.longCol(name);
			case stringType: return db.string(name);
			case entityType: return db.longCol(name);
			default: return null;
			// @formatter:on
		}
	}
}
