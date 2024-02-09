package com.braintribe.gm.cli.posix.parser.api;

import com.braintribe.model.generic.GenericEntity;

public interface CliEntityEvaluator {
	boolean isEvaluable(GenericEntity entity);
	GenericEntity evaluate(GenericEntity entity);
}
