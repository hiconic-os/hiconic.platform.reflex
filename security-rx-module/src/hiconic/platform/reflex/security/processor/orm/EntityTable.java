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

import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.gm.jdbc.api.GmColumn;
import com.braintribe.gm.jdbc.api.GmColumnBuilder;
import com.braintribe.gm.jdbc.api.GmDb;
import com.braintribe.gm.jdbc.api.GmTable;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.meta.cmd.CmdResolver;

public class EntityTable {
	private final EntitySchema schema;
	private final EntityType<?> entityType;
	private GmDb db;
	private GmTable table;
	
	private final Map<String, GmColumn<?>> columns = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public EntityTable(EntitySchema schema, EntityType<?> entityType, CmdResolver cmdResolver) {
		this.schema = schema;
		this.entityType = entityType;
		
		for (Property property: entityType.getProperties()) {
			
			GenericModelType propertyType = property.getType();
	
			
			final GmColumnBuilder<?> builder; 
			
			if (property.isIdentifier()) {
				builder = db.longCol(property.getName());
			}
			else {
				builder = EntityMappings.builder(db, property);
			}
			
			if (builder != null) {
				columns.put(property.getName(), builder.done());
			}
		}
	}
}
