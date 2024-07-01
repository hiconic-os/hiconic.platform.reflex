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
package com.braintribe.gm.cli.posix.parser;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;

public abstract class AbstractCommandLineParser {
	private static final DateTimeFormatter DATETIME_FORMATTER = 
			new DateTimeFormatterBuilder().optionalStart().appendPattern("yyyy-MM-dd['T'HH[:mm[:ss[.SSS]]]][Z]").optionalEnd()
			.optionalStart().appendPattern("yyyyMMdd['T'HH[mm[ss[SSS]]]][Z]").optionalEnd()
		            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
		            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
		            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
		            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
		            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
		            .toFormatter();

	
	private static Map<EntityType<?>, Function<String, Object>> typeExperts = new HashMap<>();

	static {
		typeExperts.put(Resource.T, AbstractCommandLineParser::createFileResource);
		typeExperts.put(FileResource.T, AbstractCommandLineParser::createFileResource);
	}
	
	private static FileResource createFileResource(String path) {
		FileResource resource = FileResource.T.create();
		File file = new File(path);
		resource.setName(file.getName());
		resource.setPath(path);
		return resource;
	}

	protected Object decodeValue(GenericModelType type, String stringValue) {
		switch (type.getTypeCode()) {
			case booleanType:
			case decimalType:
			case doubleType:
			case floatType:
			case integerType:
			case longType:
			case enumType:
				return ((ScalarType) type).instanceFromString(stringValue);

			case stringType:
			case objectType:
				return stringValue;

			case entityType:
				Function<String, Object> converter = typeExperts.get(type);
				if (converter == null)
					throw new IllegalArgumentException("type " + type.getTypeSignature() + " is not supported for command line");

				return converter.apply(stringValue);

			case dateType:
				return parseDate(stringValue);
				
			case listType:
			case setType:
				LinearCollectionType collectionType = (LinearCollectionType) type;
				GenericModelType elementType = collectionType.getCollectionElementType();
				return Stream.of(stringValue.split(",")).map(t -> decodeValue(elementType, t))
						.collect(Collectors.toCollection(collectionType::createPlain));
				
			default:
				throw new IllegalArgumentException("type " + type.getTypeSignature() + " is not supported for command line");
		}
	}
	
	private static Date parseDate(String date) {
		ZonedDateTime result = ZonedDateTime.parse(date, DATETIME_FORMATTER);
		return Date.from(result.toInstant());
	}



}
