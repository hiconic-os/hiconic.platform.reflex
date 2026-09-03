package hiconic.rx.webapp.development.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WebAppContributionReader {
	public static final String CONTRIBUTION_PATH = "HICONIC-CONF/webapp-dependencies.properties";

	private static final Pattern DEPENDENCY = Pattern.compile("[^:#\\s]+:[^#\\s]+#[^\\s]+");
	private static final Pattern PART = Pattern.compile("[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+)?");

	public List<WebAppContribution> read(ClassLoader classLoader) {
		try {
			List<URL> resources = enumeration(classLoader.getResources(CONTRIBUTION_PATH));
			resources.sort(Comparator.comparing(URL::toExternalForm));

			Map<String, WebAppContribution> byServerPath = new LinkedHashMap<>();
			for (URL resource : resources)
				read(resource, byServerPath);
			return byServerPath.values().stream().sorted(Comparator.comparing(WebAppContribution::serverPath)).toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read development web application contributions", e);
		}
	}

	private void read(URL resource, Map<String, WebAppContribution> byServerPath) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
			int lineNumber = 0;
			for (String line; (line = reader.readLine()) != null;) {
				lineNumber++;
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
					continue;

				WebAppContribution contribution = parse(trimmed, resource + ":" + lineNumber);
				WebAppContribution previous = byServerPath.putIfAbsent(contribution.serverPath(), contribution);
				if (previous != null && !previous.sameMapping(contribution))
					throw new IllegalStateException("Conflicting development web applications for path '" + contribution.serverPath()
							+ "': " + previous.source() + " and " + contribution.source());
			}
		}
	}

	public WebAppContribution parse(String line, String source) {
		int equals = line.indexOf('=');
		if (equals <= 0 || equals == line.length() - 1)
			throw invalid(source, line);

		String dependencyAndPart = line.substring(0, equals).trim();
		String mapping = line.substring(equals + 1).trim();
		String welcome = null;

		int optionStart = mapping.indexOf(';');
		if (optionStart >= 0) {
			String options = mapping.substring(optionStart + 1);
			mapping = mapping.substring(0, optionStart).trim();
			for (String token : options.split(";")) {
				String option = token.trim();
				if (option.startsWith("welcome="))
					welcome = nonEmpty(option.substring("welcome=".length()), "welcome file", source);
				else if (!option.isEmpty())
					throw new IllegalArgumentException("Unknown web application option '" + option + "' at " + source);
			}
		}

		int partSeparator = dependencyAndPart.lastIndexOf('/');
		if (partSeparator <= 0 || partSeparator == dependencyAndPart.length() - 1)
			throw invalid(source, line);

		String dependency = dependencyAndPart.substring(0, partSeparator);
		String part = dependencyAndPart.substring(partSeparator + 1);
		if (!DEPENDENCY.matcher(dependency).matches() || !PART.matcher(part).matches())
			throw invalid(source, line);

		return new WebAppContribution(dependency, part, normalizeServerPath(mapping, source), welcome, source);
	}

	private static String normalizeServerPath(String path, String source) {
		String result = nonEmpty(path, "server path", source).replace('\\', '/');
		if (!result.startsWith("/"))
			result = "/" + result;
		while (result.endsWith("/") && result.length() > 1)
			result = result.substring(0, result.length() - 1);
		if (result.equals("/") || result.contains("/../") || result.endsWith("/..") || result.contains("/./"))
			throw new IllegalArgumentException("Illegal web application path at " + source + ": " + path);
		return result;
	}

	private static String nonEmpty(String value, String label, String source) {
		String result = value.trim();
		if (result.isEmpty())
			throw new IllegalArgumentException("Empty " + label + " at " + source);
		return result;
	}

	private static IllegalArgumentException invalid(String source, String line) {
		return new IllegalArgumentException("Invalid web application contribution at " + source + ": " + line);
	}

	private static <T> List<T> enumeration(Enumeration<T> enumeration) {
		List<T> result = new ArrayList<>();
		while (enumeration.hasMoreElements())
			result.add(enumeration.nextElement());
		return result;
	}
}
