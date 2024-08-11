package com.braintribe.gm.cli.posix.parser;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;

public class LinearCollectionAccumulator extends CollectionAccumulator {

	private GenericModelType elementType;
	private LinearCollectionType linearCollectionType;

	public LinearCollectionAccumulator(CliArgumentParser parser, Consumer<Object> consumer, LinearCollectionType collectionType) {
		super(parser, consumer, collectionType);
		this.linearCollectionType = collectionType;
		elementType = collectionType.getCollectionElementType();
	}
	
	@Override
	protected GenericModelType inferredType() {
		return elementType;
	}

	@Override
	protected Object buildValue(List<CliArgument> args) {
		Collection<Object> collection = linearCollectionType.createPlain();
		
		for (CliArgument arg: args) {
			Object value = arg.getValue();
			if (!elementType.isInstance(value))
				throw new ParseException("Error at " + arg.getDesc() + ": Collection element type mismatch");
				
			collection.add(value);
		}
		return collection;
	}

}
