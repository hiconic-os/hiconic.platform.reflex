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

	// TODO move this somewhere?
	private static final DateTimeFormatter DATETIME_FORMATTER = 
			new DateTimeFormatterBuilder()
			.optionalStart().appendPattern("yyyy-MM-dd['T'HH[:mm[:ss[.SSS]]]][X]").optionalEnd()
			.optionalStart().appendPattern("yyyyMMdd['T'HH[mm[ss[SSS]]]][X]").optionalEnd()
		            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
		            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
		            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
		            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
		            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
		            .toFormatter();

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
		ZonedDateTime result = ZonedDateTime.parse(date, DATETIME_FORMATTER);
		return Date.from(result.toInstant());
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
