package hiconic.rx.webapi.client.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class PathTemplateResolverTest {

	@Test
	public void omitsTrailingOptionalSegments() {
		Map<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("first", "one");
		assertEquals("/foo/one", PathTemplateResolver.resolve("/foo/{first}/{second}", parameters, Set.of("second")));
		assertEquals("/foo", PathTemplateResolver.resolve("/foo/{first}/{second}", Collections.emptyMap(), Set.of("first", "second")));
	}

	@Test
	public void omitsLiteralDelimitedMiddleSegment() {
		assertEquals("/foo/fix/two", PathTemplateResolver.resolve("/foo/{first}/fix/{second}",
				Collections.singletonMap("second", "two"), Set.of("first")));
	}

	@Test
	public void rejectsAmbiguousAndEmbeddedOptionalSegments() {
		assertRejected("/foo/{first}/{second}", Collections.singletonMap("second", "two"), Set.of("first"));
		assertRejected("/foo/prefix-{first}", Collections.emptyMap(), Set.of("first"));
	}

	private static void assertRejected(String template, Map<String, Object> parameters, Set<String> omitted) {
		try {
			PathTemplateResolver.resolve(template, parameters, omitted);
			fail("Expected invalid optional path template to be rejected");
		} catch (IllegalArgumentException expected) {
			// expected
		}
	}
}
