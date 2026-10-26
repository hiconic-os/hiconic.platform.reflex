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

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.Test;

public class HttpRequestEntityDecoderUtilsTest {

	@Test
	public void parsesCanonicalDateTimeWithColonOffset() {
		assertParsesAs("1992-10-10T10:10:10.777+00:30", "1992-10-10T09:40:10.777Z");
	}

	@Test
	public void parsesCanonicalDateTimeWithUtcDesignator() {
		assertParsesAs("1992-10-10T10:10:10.777Z", "1992-10-10T10:10:10.777Z");
	}

	@Test
	public void adaptsLegacyExtendedDateTimeWithoutColonOffset() {
		assertParsesAs("1992-10-10T10:10:10.777+0030", "1992-10-10T09:40:10.777Z");
	}

	@Test
	public void adaptsLegacyCompactDateTime() {
		assertParsesAs("19921010T101010777+0030", "1992-10-10T09:40:10.777Z");
	}

	@Test(expected = DateTimeParseException.class)
	public void rejectsUnsupportedDateTime() {
		HttpRequestEntityDecoderUtils.parseDate("1992/10/10 10:10:10");
	}

	private static void assertParsesAs(String input, String expectedInstant) {
		assertEquals(Date.from(Instant.parse(expectedInstant)), HttpRequestEntityDecoderUtils.parseDate(input));
	}
}
