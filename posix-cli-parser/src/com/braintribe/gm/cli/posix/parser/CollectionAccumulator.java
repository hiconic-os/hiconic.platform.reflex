package com.braintribe.gm.cli.posix.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.CollectionType;
import com.braintribe.model.generic.reflection.GenericModelType;

public abstract class CollectionAccumulator {
	private List<CliArgument> arguments = new ArrayList<>();
	
	private int forwardCount = 0;
	private Consumer<Object> consumer;
	protected int index;

	private CollectionType collectionType;

	private CliArgumentParser parser;
	
	protected abstract GenericModelType inferredType();
	protected abstract Object buildValue(List<CliArgument> values);
	
	protected CollectionAccumulator(CliArgumentParser parser, Consumer<Object> consumer, CollectionType collectionType) {
		this.parser = parser;
		this.consumer = consumer;
		this.collectionType = collectionType;
	}
	
	public void parse() {
		while (true) {
			CliArgument argument = parser.parseOptionalArgumentValue(inferredType());
			
			if (argument == null)
				break;
			
			arguments.add(argument);
			
			Object value = argument.getValue();
			
			if (value != null && value instanceof ForwardReference fr) {
				forwardCount++;
				fr.consumers.add(v -> resolve(argument, v));
			}
			
			index++;
		}
		
		buildAndNotifyIfComplete();
	}
	
	private void resolve(CliArgument argument, Object value) {
		argument.setValue(value);
		forwardCount--;
		buildAndNotifyIfComplete();
	}
	
	private void buildAndNotifyIfComplete() {
		if (forwardCount == 0) {
			if (arguments.size() == 1) {
				CliArgument argument = arguments.get(0);
				Object value = argument.getValue();
				if (collectionType.isInstance(value)) {
					consumer.accept(value);
					return;
				}
			}
			
			Object value = buildValue(arguments);
			consumer.accept(value);
		}
	}
	
}
