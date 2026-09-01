// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.processing.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.processing.resource.artifact.ArtifactResourceValueDescriptorExperts;
import com.braintribe.model.processing.resource.artifact.api.ArtifactResourceResolver;
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
		return new ModelBasedValueDescriptorExpressionCodec(ImportText.T,
				com.braintribe.model.resource.source.vd.ArtifactResourceSource.T,
				com.braintribe.model.resource.source.vd.ArtifactResource.T,
				hiconic.rx.resource.model.packaged.vd.PackagedResourceSource.T, PackagedResource.T);
	}

	public static void register(ValueDescriptorExpertRegistry registry, RxPackagedResourceResolver resolver) {
		ArtifactResourceValueDescriptorExperts.register(registry, artifactResolver(resolver));

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

		registry.register(hiconic.rx.resource.model.packaged.vd.PackagedResourceSource.T, (context, descriptor) -> {
			Maybe<ResolvedPath> resolved = resolve(context.getAspect(ValueDescriptorSourceContext.class),
					descriptor.getArtifact(), descriptor.getPath());
			if (resolved.isUnsatisfied())
				return resolved.whyUnsatisfied().asMaybe();

			ResolvedPath path = resolved.get();
			try {
				return Maybe.complete(resolver.resource(path.artifact, path.path).asSource());
			} catch (IllegalArgumentException e) {
				return NotFound.create("Indexed packaged resource not found: " + path).asMaybe();
			}
		});
	}

	/**
	 * Losslessly projects packaged resource sources relative to the document being written. The surrounding
	 * {@link Resource}, including all of its metadata, remains modeled explicitly.
	 */
	public static ValueDescriptorExpressionProjection projection(ValueDescriptorSourceContext target) {
		return ArtifactResourceValueDescriptorExperts.projection(target);
	}

	/**
	 * Projects packaged resource sources and, when explicitly approved by the caller, entire regenerable resources.
	 * The predicate is deliberately external: source shape alone is not proof that resource metadata may be discarded.
	 */
	public static ValueDescriptorExpressionProjection projection(ValueDescriptorSourceContext target,
			Predicate<? super Resource> regenerableResource) {
		return ArtifactResourceValueDescriptorExperts.projection(target, regenerableResource, source -> false);
	}

	private static ArtifactResourceResolver artifactResolver(RxPackagedResourceResolver resolver) {
		if (resolver instanceof ArtifactResourceResolver)
			return (ArtifactResourceResolver) resolver;

		return new ArtifactResourceResolver() {
			@Override
			public Maybe<Resource> resolveResource(String artifact, String path) {
				try {
					return Maybe.complete(resolver.resource(artifact, path).asPersistableResource());
				} catch (IllegalArgumentException e) {
					return NotFound.create(e.getMessage()).asMaybe();
				}
			}

			@Override
			public Maybe<com.braintribe.model.resource.source.ArtifactResourceSource> resolveSource(String artifact, String path) {
				try {
					return Maybe.complete(resolver.resource(artifact, path).asSource());
				} catch (IllegalArgumentException e) {
					return NotFound.create(e.getMessage()).asMaybe();
				}
			}
		};
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
