// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package dev.hiconic.servlet.decoder.impl;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.Property;

public class HttpRequestEntityDecoderUtils {

	private static final Set<Property> INVALID_HEADER_PROPERTIES = new HashSet<>(GenericEntity.T.getDeclaredProperties());

	private static final String CANONICAL_DATE_TIME_PATTERN = "yyyy-MM-dd['T'HH[:mm[:ss[.SSS]]]][XXX]";
	private static final String LEGACY_EXTENDED_DATE_TIME_PATTERN = "yyyy-MM-dd['T'HH[:mm[:ss[.SSS]]]][X]";
	private static final String LEGACY_COMPACT_DATE_TIME_PATTERN = "yyyyMMdd['T'HH[mm[ss[SSS]]]][X]";

	/* The first formatter represents the ideal, canonical RFC-3339-style input. The remaining formatters are adaptive compatibility variants
	 * for legacy clients that still use an offset without a colon or the compact notation; they are not equally preferred wire representations.
	 * DateTimeFormatter optional sections are sequential optional grammar parts, not alternatives, so every representation intentionally has
	 * its own formatter and is tried in a deterministic order. */
	private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of( //
			dateTimeFormatter(CANONICAL_DATE_TIME_PATTERN), // RFC-3339-style offset, e.g. +00:30
			dateTimeFormatter(LEGACY_EXTENDED_DATE_TIME_PATTERN), // legacy offset, e.g. +0030
			dateTimeFormatter(LEGACY_COMPACT_DATE_TIME_PATTERN));

	/**
	 * Parse the given date.
	 * 
	 * @param date the string to parse, must not be {@code null}
	 * @return the parsed date
	 * @throws DateTimeParseException if the string has a wrong format
	 * 
	 * @see ZonedDateTime#parse
	 */
	public static Date parseDate(String date) {
		DateTimeParseException lastFailure = null;
		for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
			try {
				ZonedDateTime result = ZonedDateTime.parse(date, formatter);
				return Date.from(result.toInstant());
			} catch (DateTimeParseException e) {
				lastFailure = e;
			}
		}

		throw new DateTimeParseException("Unsupported date/time value. Expected " + CANONICAL_DATE_TIME_PATTERN + " or one of the legacy formats "
				+ LEGACY_EXTENDED_DATE_TIME_PATTERN + ", " + LEGACY_COMPACT_DATE_TIME_PATTERN, date,
				lastFailure == null ? 0 : lastFailure.getErrorIndex(), lastFailure);
	}

	private static DateTimeFormatter dateTimeFormatter(String pattern) {
		return new DateTimeFormatterBuilder() //
				.appendPattern(pattern) //
				.parseDefaulting(ChronoField.HOUR_OF_DAY, 0) //
				.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0) //
				.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0) //
				.parseDefaulting(ChronoField.NANO_OF_SECOND, 0) //
				.parseDefaulting(ChronoField.OFFSET_SECONDS, 0) //
				.toFormatter();
	}
	
	public static String serializeDate(Date date) {		
		Instant instant = date.toInstant();
		return DateTimeFormatter.ISO_INSTANT.format(instant);
	}
	
	public static List<Property> filterOutInvalidHeaderProperties(List<Property> properties) {
		return properties.stream().filter(property -> !INVALID_HEADER_PROPERTIES.contains(property)).collect(Collectors.toList());
	}
	
	public static void illegalArgument(String message, Object ...values) {
		throw new IllegalArgumentException(String.format(message, values));
	}

	public static void checkArgumentNotNull(Object arg, String name) {
		if(arg == null) {
			illegalArgument("The %s must not be null", name);
		}
	}
}
