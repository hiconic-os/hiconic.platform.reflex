// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.processing.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.processing.vde.expression.ModelBasedValueDescriptorExpressionCodec;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionCodec;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionProjection;
import com.braintribe.model.processing.vde.reasoned.api.ValueDescriptorSourceContext;
import com.braintribe.model.processing.vde.reasoned.impl.ValueDescriptorExpertRegistry;
import com.braintribe.model.resource.Resource;

import hiconic.rx.module.api.resource.RxPackagedResourceResolver;
import hiconic.rx.resource.model.packaged.vd.ImportText;
import hiconic.rx.resource.model.packaged.vd.PackagedResource;

/** Model vocabulary and reasoned experts for configuration-relative packaged resources. */
public final class PackagedResourceValueDescriptorExperts {

	private PackagedResourceValueDescriptorExperts() {
	}

	public static ValueDescriptorExpressionCodec expressionCodec() {
		return new ModelBasedValueDescriptorExpressionCodec(ImportText.T, PackagedResource.T);
	}

	public static void register(ValueDescriptorExpertRegistry registry, RxPackagedResourceResolver resolver) {
		registry.register(ImportText.T, (context, descriptor) -> {
			Maybe<ResolvedPath> resolved = resolve(context.getAspect(ValueDescriptorSourceContext.class),
					descriptor.getArtifact(), descriptor.getPath());
			if (resolved.isUnsatisfied())
				return resolved.whyUnsatisfied().asMaybe();

			ResolvedPath path = resolved.get();
			try (InputStream in = resolver.resource(path.artifact, path.path).asHandle().asStream()) {
				return Maybe.complete(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			} catch (IllegalArgumentException e) {
				return NotFound.create("Indexed text resource not found: " + path).asMaybe();
			} catch (IOException e) {
				return InternalError.from(e).asMaybe();
			}
		});

		registry.register(PackagedResource.T, (context, descriptor) -> {
			Maybe<ResolvedPath> resolved = resolve(context.getAspect(ValueDescriptorSourceContext.class),
					descriptor.getArtifact(), descriptor.getPath());
			if (resolved.isUnsatisfied())
				return resolved.whyUnsatisfied().asMaybe();

			ResolvedPath path = resolved.get();
			try {
				Resource resource = resolver.resource(path.artifact, path.path).asPersistableResource();
				return Maybe.complete(resource);
			} catch (IllegalArgumentException e) {
				return NotFound.create("Indexed packaged resource not found: " + path).asMaybe();
			}
		});
	}

	/** Projects canonical packaged resources back to compact expressions relative to the document being written. */
	public static ValueDescriptorExpressionProjection projection(ValueDescriptorSourceContext target) {
		return (inferredType, value) -> {
			if (!(value instanceof Resource resource) || !(resource.getResourceSource() instanceof hiconic.rx.resource.model.packaged.PackagedResourceSource source))
				return null;
			if (source.getArtifact() == null || source.getArtifact().isBlank() || source.getPath() == null || source.getPath().isBlank())
				return null;

			PackagedResource descriptor = PackagedResource.T.create();
			if (target != null && source.getArtifact().equals(target.artifact()) && target.path() != null && !target.path().isBlank()) {
				descriptor.setPath(relativePath(target.path(), source.getPath()));
			} else {
				descriptor.setArtifact(source.getArtifact());
				descriptor.setPath(source.getPath());
			}
			return descriptor;
		};
	}

	private static String relativePath(String documentPath, String resourcePath) {
		List<String> document = segments(documentPath);
		List<String> resource = segments(resourcePath);
		if (!document.isEmpty())
			document.remove(document.size() - 1);

		int common = 0;
		while (common < document.size() && common < resource.size() && document.get(common).equals(resource.get(common)))
			common++;

		List<String> result = new ArrayList<>();
		for (int i = common; i < document.size(); i++)
			result.add("..");
		result.addAll(resource.subList(common, resource.size()));

		String relative = String.join("/", result);
		return relative.startsWith("../") || relative.equals("..") ? relative : "./" + relative;
	}

	private static List<String> segments(String path) {
		List<String> result = new ArrayList<>();
		for (String segment : path.replace('\\', '/').split("/"))
			if (!segment.isEmpty() && !segment.equals("."))
				result.add(segment);
		return result;
	}

	private static Maybe<ResolvedPath> resolve(ValueDescriptorSourceContext source, String explicitArtifact, String configuredPath) {
		if (configuredPath == null || configuredPath.isBlank())
			return InvalidArgument.create("A packaged resource expression requires a non-empty path").asMaybe();

		String artifact = explicitArtifact;
		String candidate = configuredPath.replace('\\', '/');
		if (artifact == null || artifact.isBlank()) {
			if (source == null || source.artifact() == null || source.artifact().isBlank())
				return InvalidArgument.create("A relative packaged resource has no owning artifact context: " + configuredPath).asMaybe();
			artifact = source.artifact();
			String sourcePath = source.path() == null ? "" : source.path().replace('\\', '/');
			int separator = sourcePath.lastIndexOf('/');
			candidate = (separator < 0 ? "" : sourcePath.substring(0, separator + 1)) + candidate;
		} else {
			while (candidate.startsWith("./"))
				candidate = candidate.substring(2);
		}

		while (candidate.startsWith("/"))
			candidate = candidate.substring(1);
		Path normalized = Paths.get(candidate).normalize();
		String normalizedPath = normalized.toString().replace('\\', '/');
		if (normalized.isAbsolute() || normalizedPath.isEmpty() || normalizedPath.equals("..") || normalizedPath.startsWith("../"))
			return InvalidArgument.create("Packaged resource path escapes its artifact: " + configuredPath).asMaybe();

		return Maybe.complete(new ResolvedPath(artifact, normalizedPath));
	}

	private static final class ResolvedPath {
		private final String artifact;
		private final String path;

		private ResolvedPath(String artifact, String path) {
			this.artifact = artifact;
			this.path = path;
		}

		@Override
		public String toString() {
			return artifact + ":" + path;
		}
	}
}
