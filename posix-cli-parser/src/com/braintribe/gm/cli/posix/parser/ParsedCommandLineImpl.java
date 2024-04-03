package com.braintribe.gm.cli.posix.parser;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.gm.cli.posix.parser.api.ParsedCommandLine;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class ParsedCommandLineImpl implements ParsedCommandLine {

	private Set<GenericEntity> entities = new LinkedHashSet<>();
	
	@Override
	public void addEntity(GenericEntity entity) {
		entities.add(entity);
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
