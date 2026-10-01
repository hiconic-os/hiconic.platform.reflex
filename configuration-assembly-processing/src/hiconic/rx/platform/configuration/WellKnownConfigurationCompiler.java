// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;

import hiconic.rx.platform.conf.LayeredConfigurationEntries;
import hiconic.rx.platform.conf.LayeredConfigurationEntries.Entry;

/** Compiles runtime-layered well-known configuration files into deterministic single-file build outputs. */
final class WellKnownConfigurationCompiler {
	private static final String CLASSPATH_CONF_PATH = "HICONIC-CONF/";

	private WellKnownConfigurationCompiler() {
	}

	static Compilation compile(ClasspathIndex sourceIndex, Path confDirectory) throws Exception {
		List<FilesystemInput> filesystemInputs = new ArrayList<>();
		compileLogLevels(sourceIndex, confDirectory, filesystemInputs);
		compileLogback(sourceIndex, confDirectory, filesystemInputs);
		return new Compilation(List.copyOf(filesystemInputs));
	}

	private static void compileLogLevels(ClasspathIndex sourceIndex, Path confDirectory,
			List<FilesystemInput> filesystemInputs) throws IOException {
		LayeredConfigurationEntries layers = new LayeredConfigurationEntries(sourceIndex, CLASSPATH_CONF_PATH,
				confDirectory.toFile(), "log-levels", ".properties");
		if (layers.entries().isEmpty())
			return;

		Map<String, String> merged = new TreeMap<>();
		for (Entry entry : layers.entries()) {
			registerFilesystemInput(entry, "log-levels.properties", filesystemInputs);
			Properties properties = new Properties();
			try (InputStream in = open(entry);
					InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
			for (String name : properties.stringPropertyNames())
				merged.put(name, properties.getProperty(name));
		}

		removeFilesystemLayers(confDirectory, "log-levels", ".properties");
		StringBuilder content = new StringBuilder();
		merged.forEach((name, value) -> content.append(escapeProperty(name, true)).append('=')
				.append(escapeProperty(value, false)).append('\n'));
		Files.writeString(confDirectory.resolve("log-levels.properties"), content, StandardCharsets.UTF_8);
	}

	private static void compileLogback(ClasspathIndex sourceIndex, Path confDirectory,
			List<FilesystemInput> filesystemInputs) throws Exception {
		LayeredConfigurationEntries layers = new LayeredConfigurationEntries(sourceIndex, CLASSPATH_CONF_PATH,
				confDirectory.toFile(), "logback", ".xml");
		if (layers.entries().isEmpty())
			return;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

		Document merged = factory.newDocumentBuilder().newDocument();
		Element mergedRoot = merged.createElement("configuration");
		merged.appendChild(mergedRoot);
		for (Entry entry : layers.entries()) {
			registerFilesystemInput(entry, "logback.xml", filesystemInputs);
			Document layer;
			try (InputStream in = open(entry)) {
				layer = factory.newDocumentBuilder().parse(in);
			}
			Element root = layer.getDocumentElement();
			if (!"configuration".equals(root.getLocalName()) && !"configuration".equals(root.getNodeName()))
				throw new IllegalArgumentException("Logback layer [" + entry.source() + "] has no <configuration> root");
			copyAttributes(root, mergedRoot);
			for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
				mergedRoot.appendChild(merged.importNode(child, true));
		}

		removeFilesystemLayers(confDirectory, "logback", ".xml");
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		var transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		try (OutputStream out = Files.newOutputStream(confDirectory.resolve("logback.xml"))) {
			transformer.transform(new DOMSource(merged), new StreamResult(out));
		}
	}

	private static void registerFilesystemInput(Entry entry, String output, List<FilesystemInput> filesystemInputs) {
		if (!entry.classpath())
			filesystemInputs.add(new FilesystemInput(output, "conf/" + entry.file().getName()));
	}

	private static InputStream open(Entry entry) throws IOException {
		return entry.classpath() ? entry.url().openStream() : Files.newInputStream(entry.file().toPath());
	}

	private static void copyAttributes(Element source, Element target) {
		NamedNodeMap attributes = source.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			String namespace = attribute.getNamespaceURI();
			if (namespace == null)
				target.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
			else
				target.setAttributeNS(namespace, attribute.getNodeName(), attribute.getNodeValue());
		}
	}

	private static void removeFilesystemLayers(Path confDirectory, String baseName, String extension) throws IOException {
		try (var files = Files.list(confDirectory)) {
			for (Path file : files.filter(Files::isRegularFile)
					.filter(path -> isLayer(path.getFileName().toString(), baseName, extension)).toList())
				Files.delete(file);
		}
	}

	static boolean isLayer(String name, String baseName, String extension) {
		return name.equals(baseName + extension) || name.startsWith(baseName + ".") && name.endsWith(extension);
	}

	private static String escapeProperty(String value, boolean key) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\\' -> result.append("\\\\");
			case '\n' -> result.append("\\n");
			case '\r' -> result.append("\\r");
			case '\t' -> result.append("\\t");
			case '=', ':', '#', '!' -> {
				if (key)
					result.append('\\');
				result.append(c);
			}
			default -> result.append(c);
			}
		}
		return result.toString();
	}

	record Compilation(List<FilesystemInput> filesystemInputs) {
	}

	record FilesystemInput(String output, String resource) {
	}
}
