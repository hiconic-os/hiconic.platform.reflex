// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.platform.loading;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal class-file reader for the fixed {@code rx/package-info.class} descriptor. */
/* package */ final class RxArtifactDescriptorReader {

	private static final String RX_ARTIFACT = "Lhiconic/rx/module/api/annotation/RxArtifact;";

	record OptionalModuleDescriptor(String module, List<String> activationDependencies) {}
	record RxArtifactDescriptor(List<String> modules, List<OptionalModuleDescriptor> optionalModules, URL source) {}
	private record AnnotationInfo(String type, Map<String, Object> values) {}

	static RxArtifactDescriptor read(URL url) throws IOException {
		try (var input = url.openStream()) {
			return read(input.readAllBytes(), url);
		}
	}

	static RxArtifactDescriptor read(byte[] bytes, URL source) throws IOException {
		try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
			if (in.readInt() != 0xCAFEBABE)
				throw new IOException("Not a class file: " + source);
			in.readUnsignedShort();
			in.readUnsignedShort();
			Object[] constants = readConstantPool(in);
			in.readUnsignedShort();
			in.readUnsignedShort();
			in.readUnsignedShort();
			skipInterfaces(in);
			skipMembers(in);
			skipMembers(in);

			for (AnnotationInfo annotation : readClassAnnotations(in, constants))
				if (RX_ARTIFACT.equals(annotation.type()))
					return toDescriptor(annotation, source);

			throw new IOException("Missing @RxArtifact on " + source);
		}
	}

	private static RxArtifactDescriptor toDescriptor(AnnotationInfo annotation, URL source) throws IOException {
		List<String> modules = classNames(annotation.values().get("modules"), "modules", source);
		List<OptionalModuleDescriptor> optionalModules = new ArrayList<>();
		for (Object value : values(annotation.values().get("optionalModules"))) {
			if (!(value instanceof AnnotationInfo optional))
				throw malformed("optionalModules must contain annotations", source);
			String module = className(optional.values().get("module"), "module", source);
			List<String> dependencies = classNames(optional.values().get("activationDependencies"), "activationDependencies", source);
			optionalModules.add(new OptionalModuleDescriptor(module, dependencies));
		}
		return new RxArtifactDescriptor(List.copyOf(modules), List.copyOf(optionalModules), source);
	}

	private static List<String> classNames(Object value, String element, URL source) throws IOException {
		List<String> result = new ArrayList<>();
		for (Object item : values(value))
			result.add(className(item, element, source));
		return result;
	}

	private static List<?> values(Object value) {
		return value == null ? List.of() : value instanceof List<?> list ? list : List.of(value);
	}

	private static String className(Object value, String element, URL source) throws IOException {
		if (!(value instanceof String descriptor) || !descriptor.startsWith("L") || !descriptor.endsWith(";"))
			throw malformed("Element '" + element + "' is not a class literal", source);
		return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
	}

	private static IOException malformed(String message, URL source) {
		return new IOException("Malformed @RxArtifact in " + source + ": " + message);
	}

	private static Object[] readConstantPool(DataInputStream in) throws IOException {
		Object[] constants = new Object[in.readUnsignedShort()];
		for (int i = 1; i < constants.length; i++) {
			int tag = in.readUnsignedByte();
			switch (tag) {
			case 1 -> {
				constants[i] = in.readUTF();
			}
			case 3, 4 -> in.skipNBytes(4);
			case 5, 6 -> { in.skipNBytes(8); i++; }
			case 7, 8, 16, 19, 20 -> in.skipNBytes(2);
			case 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
			case 15 -> in.skipNBytes(3);
			default -> throw new IOException("Unsupported class-file constant-pool tag " + tag);
			}
		}
		return constants;
	}

	private static void skipInterfaces(DataInputStream in) throws IOException {
		in.skipNBytes(2L * in.readUnsignedShort());
	}

	private static void skipMembers(DataInputStream in) throws IOException {
		for (int i = 0, count = in.readUnsignedShort(); i < count; i++) {
			in.skipNBytes(6);
			skipAttributes(in);
		}
	}

	private static void skipAttributes(DataInputStream in) throws IOException {
		for (int i = 0, count = in.readUnsignedShort(); i < count; i++) {
			in.readUnsignedShort();
			in.skipNBytes(Integer.toUnsignedLong(in.readInt()));
		}
	}

	private static List<AnnotationInfo> readClassAnnotations(DataInputStream in, Object[] constants) throws IOException {
		List<AnnotationInfo> result = new ArrayList<>();
		for (int i = 0, count = in.readUnsignedShort(); i < count; i++) {
			String name = utf(constants, in.readUnsignedShort());
			int length = in.readInt();
			if ("RuntimeInvisibleAnnotations".equals(name) || "RuntimeVisibleAnnotations".equals(name)) {
				byte[] attribute = in.readNBytes(length);
				try (var annotations = new DataInputStream(new ByteArrayInputStream(attribute))) {
					for (int a = 0, annotationCount = annotations.readUnsignedShort(); a < annotationCount; a++)
						result.add(readAnnotation(annotations, constants));
				}
			} else {
				in.skipNBytes(Integer.toUnsignedLong(length));
			}
		}
		return result;
	}

	private static AnnotationInfo readAnnotation(DataInputStream in, Object[] constants) throws IOException {
		String type = utf(constants, in.readUnsignedShort());
		Map<String, Object> values = new LinkedHashMap<>();
		for (int i = 0, count = in.readUnsignedShort(); i < count; i++)
			values.put(utf(constants, in.readUnsignedShort()), readElementValue(in, constants));
		return new AnnotationInfo(type, values);
	}

	private static Object readElementValue(DataInputStream in, Object[] constants) throws IOException {
		return switch (in.readUnsignedByte()) {
		case 'c', 's', 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> utfOrConstant(constants, in.readUnsignedShort());
		case 'e' -> { in.readUnsignedShort(); yield utfOrConstant(constants, in.readUnsignedShort()); }
		case '@' -> readAnnotation(in, constants);
		case '[' -> {
			List<Object> values = new ArrayList<>();
			for (int i = 0, count = in.readUnsignedShort(); i < count; i++)
				values.add(readElementValue(in, constants));
			yield values;
		}
		default -> throw new IOException("Unsupported annotation element value");
		};
	}

	private static Object utfOrConstant(Object[] constants, int index) throws IOException {
		Object value = constants[index];
		if (value == null)
			throw new IOException("Unsupported annotation constant at index " + index);
		return value;
	}

	private static String utf(Object[] constants, int index) throws IOException {
		Object value = constants[index];
		if (!(value instanceof String string))
			throw new IOException("Expected UTF-8 constant at index " + index);
		return string;
	}

	private RxArtifactDescriptorReader() {}
}
