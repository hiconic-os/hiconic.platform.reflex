package hiconic.platform.reflex.security.processor.orm;

import java.util.LinkedHashMap;
import java.util.Map;

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
	
	private Map<String, GmColumn<?>> columns = new LinkedHashMap<>();
	
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
