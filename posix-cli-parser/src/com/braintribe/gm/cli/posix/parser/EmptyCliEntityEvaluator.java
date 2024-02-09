package com.braintribe.gm.cli.posix.parser;

import com.braintribe.gm.cli.posix.parser.api.CliEntityEvaluator;
import com.braintribe.model.generic.GenericEntity;

public class EmptyCliEntityEvaluator implements CliEntityEvaluator {
	public static final CliEntityEvaluator INSTANCE = new EmptyCliEntityEvaluator();
	
	@Override
	public GenericEntity evaluate(GenericEntity entity) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isEvaluable(GenericEntity entity) {
		return false;
	}
}
