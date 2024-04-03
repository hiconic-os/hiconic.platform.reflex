package com.braintribe.gm.cli.posix.parser.api;

import java.util.List;
import java.util.Optional;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public interface ParsedCommandLine {
	void addEntity(GenericEntity entity);

	<O extends GenericEntity> List<O> listInstances(EntityType<O> optionsType);
	<O extends GenericEntity> Optional<O> findInstance(EntityType<O> optionsType);
}
