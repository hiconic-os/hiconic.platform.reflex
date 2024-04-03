package hiconic.rx.cli.processing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class EntityFactory implements Function<EntityType<?>, GenericEntity> {
	private Map<EntityType<?>, GenericEntity> singletons = new HashMap<>();
	
	public <S extends GenericEntity> void registerSingleton(EntityType<S> singletonType, S singleton) {
		singletons.put(singletonType, singleton);
	}
	
	@Override
	public GenericEntity apply(EntityType<?> type) {
		GenericEntity entity = singletons.get(type);
		
		if (entity != null)
			return entity;
		
		return type.create();
	}
}
