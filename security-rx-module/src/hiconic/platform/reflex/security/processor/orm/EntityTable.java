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
	private EntitySchema schema;
	private EntityType<?> entityType;
	private GmDb db;
	private GmTable table;
	
	private Map<String, GmColumn<?>> columns = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public EntityTable(EntitySchema schema, EntityType<?> entityType, CmdResolver cmdResolver) {
		super();
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
