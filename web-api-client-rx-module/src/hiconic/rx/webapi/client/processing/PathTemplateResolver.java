package hiconic.rx.webapi.client.processing;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.utils.StringTools;

final class PathTemplateResolver {

	private PathTemplateResolver() {
	}

	static String resolve(String template, Map<String, Object> parameters, Set<String> nullSegmentsToOmit) {
		return StringTools.patternFormat(omitNullSegments(template, nullSegmentsToOmit), parameters);
	}

	private static String omitNullSegments(String template, Set<String> parameterNames) {
		if (parameterNames.isEmpty())
			return template;

		for (String parameterName : parameterNames) {
			String placeholder = "{" + parameterName + "}";
			boolean exactSegment = Arrays.stream(template.split("/", -1)).anyMatch(placeholder::equals);
			if (!exactSegment && template.contains(placeholder))
				throw new IllegalArgumentException("Optional path parameter '" + parameterName
						+ "' must occupy a complete path segment in template '" + template + "'.");
		}
		validatePositionalAmbiguity(template, parameterNames);

		return Arrays.stream(template.split("/", -1))
				.filter(segment -> !isOmittedPlaceholder(segment, parameterNames))
				.collect(Collectors.joining("/"));
	}

	private static void validatePositionalAmbiguity(String template, Set<String> omittedParameterNames) {
		String[] segments = template.split("/", -1);
		for (int i = 0; i < segments.length; i++) {
			if (!isOmittedPlaceholder(segments[i], omittedParameterNames))
				continue;
			for (int j = i + 1; j < segments.length && isPlaceholder(segments[j]); j++) {
				String laterName = segments[j].substring(1, segments[j].length() - 1);
				if (!omittedParameterNames.contains(laterName))
					throw new IllegalArgumentException("Path parameter '" + laterName + "' cannot be present after omitted optional parameter '"
							+ segments[i].substring(1, segments[i].length() - 1) + "' in template '" + template + "'.");
			}
		}
	}

	private static boolean isPlaceholder(String segment) {
		return segment.length() > 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}';
	}

	private static boolean isOmittedPlaceholder(String segment, Set<String> parameterNames) {
		return isPlaceholder(segment)
				&& parameterNames.contains(segment.substring(1, segment.length() - 1));
	}
}
