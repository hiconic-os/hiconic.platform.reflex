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

import com.braintribe.gm.jdbc.api.GmColumn;
import com.braintribe.gm.jdbc.api.GmDb;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;

public class CollectionTable {
	public EntitySchema schema;
	public EntityType<?> entityType;
	public Property property;
	public GmDb db;
	
	public GmColumn<?> owner;
	public GmColumn<?> value;

	public CollectionTable(EntitySchema schema, EntityType<?> entityType, String property) {
		this(schema, entityType, entityType.getProperty(property));
	}
	
	public CollectionTable(EntitySchema schema, EntityType<?> entityType, Property property) {
		super();
		this.schema = schema;
		this.entityType = entityType;
		
		GenericModelType valueType = property.getType();
		
		owner = db.longCol("owner").notNull().done();
		value = EntityMappings.builder(db, valueType, "value").notNull().done();
	}
}
