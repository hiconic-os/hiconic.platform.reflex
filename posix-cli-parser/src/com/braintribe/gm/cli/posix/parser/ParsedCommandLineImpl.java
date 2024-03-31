package com.braintribe.gm.cli.posix.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.braintribe.gm.cli.posix.parser.api.ParsedCommandLine;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class ParsedCommandLineImpl implements ParsedCommandLine {

	private List<GenericEntity> entities = new ArrayList<>();
	private Map<EntityType<?>, GenericEntity> entitiesByType = new ConcurrentHashMap<>();
	
	@Override
	public void addEntity(GenericEntity entity) {
		entities.add(entity);
		entitiesByType.put(entity.entityType(), entity);
	}
	
	@Override
	public <O extends GenericEntity> O acquireInstance(EntityType<O> optionsType) {
		return (O) entitiesByType.computeIfAbsent(optionsType, t -> t.create());
	}
	
	@Override
	public <O extends GenericEntity> Optional<O> findInstance(EntityType<O> optionsType) {
		return (Optional<O>) entities.stream().filter(optionsType::isInstance).findFirst();
	}
	
	@Override
	public <O extends GenericEntity> List<O> listInstances(EntityType<O> optionsType) {
		return (List<O>) entities.stream().filter(optionsType::isInstance).collect(Collectors.toList());
	}
}
