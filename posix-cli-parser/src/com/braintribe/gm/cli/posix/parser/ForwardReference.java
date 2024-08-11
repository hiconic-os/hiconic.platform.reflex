package com.braintribe.gm.cli.posix.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ForwardReference {
	public List<Consumer<Object>> consumers = new ArrayList<>(2);
}
