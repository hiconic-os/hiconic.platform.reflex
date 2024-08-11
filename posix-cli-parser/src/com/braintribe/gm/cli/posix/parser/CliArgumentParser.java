package com.braintribe.gm.cli.posix.parser;

import java.util.List;

import com.braintribe.model.generic.reflection.GenericModelType;

public interface CliArgumentParser {
	
	default CliArgument parseOptionalArgumentValue(GenericModelType inferredType) {
		return parseOptionalArgumentValue(inferredType, null);
	}
	
	CliArgument parseOptionalArgumentValue(GenericModelType inferredType, List<String> positiveValues);
	
	CliArgument parseArgumentValue(GenericModelType inferredType);
	
	ParseException parseError(String msg);
}
