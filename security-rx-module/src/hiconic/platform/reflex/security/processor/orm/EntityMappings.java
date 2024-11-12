package hiconic.platform.reflex.security.processor.orm;

import com.braintribe.gm.jdbc.api.GmColumnBuilder;
import com.braintribe.gm.jdbc.api.GmDb;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;

public class EntityMappings {
	public static <T> GmColumnBuilder<?> builder(GmDb db, Property property) {
		GenericModelType propertyType = property.getType();
		
		return builder(db, propertyType, property.getName());
	}
	
	public static <T> GmColumnBuilder<?> builder(GmDb db, GenericModelType type, String name) {
		
		switch (type.getTypeCode()) {
		case booleanType: return db.booleanCol(name);
		case dateType: return db.date(name);
		case decimalType: return db.bigDecimal(name);
		case doubleType: return db.doubleCol(name);
		case enumType: return db.enumCol(name, (Class<Enum>)type.getJavaType());
		case floatType: return db.floatCol(name);
		case integerType: return db.intCol(name);
		case longType: return db.longCol(name);
		case stringType: return db.string(name);
		case entityType: return db.longCol(name);
		default: return null;
		}
	}
}
