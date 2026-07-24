// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.module.api.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Common display-name convention for technical identifiers. */
public final class DisplayNames {

	private DisplayNames() {
	}

	/**
	 * Converts camel-case and separator-delimited identifiers to title-cased words.
	 * <p>
	 * Examples: {@code fooBarX -> Foo Bar X}, {@code fix-fox-fancy -> Fix Fox Fancy},
	 * {@code fooBar-FunFact -> Foo Bar Fun Fact}.
	 */
	public static String fromTechnicalName(String technicalName) {
		if (technicalName == null || technicalName.isBlank())
			return "Unnamed";

		String words = technicalName.trim() //
				.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2") // URLValue -> URL Value
				.replaceAll("([a-z0-9])([A-Z])", "$1 $2") // fooBarX -> foo Bar X
				.replaceAll("[-_.:/]+", " ") // technical separators
				.replaceAll("\\s+", " ") //
				.trim();

		if (words.isEmpty())
			return technicalName;

		return Arrays.stream(words.split(" ")) //
				.map(DisplayNames::capitalize) //
				.collect(Collectors.joining(" "));
	}

	private static String capitalize(String word) {
		if (word.isEmpty() || word.equals(word.toUpperCase()))
			return word;
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}
}
