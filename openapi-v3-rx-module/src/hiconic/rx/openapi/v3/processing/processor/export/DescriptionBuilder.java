package hiconic.rx.openapi.v3.processing.processor.export;

import java.util.function.Consumer;

public class DescriptionBuilder implements Consumer<String> {
	private StringBuilder builder;

	@Override
	public void accept(String text) {
		add(text);
	}

	public void add(String text) {
		if (text == null)
			return;
		if (builder == null)
			builder = new StringBuilder();
		builder.append(text);
	}

	public String asString() {
		return builder == null ? null : builder.toString();
	}
}
