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
package hiconic.rx.platform.processing.resource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.mimetype.PlatformMimeTypeDetector;
import com.braintribe.model.processing.resource.packaged.api.PackagedResourceResolver;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.api.ResourceHandle;
import com.braintribe.model.resource.specification.RasterImageSpecification;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;

import hiconic.rx.module.api.resource.RxPackagedResourceBuilder;
import hiconic.rx.module.api.resource.RxPackagedResourceEntry;
import hiconic.rx.module.api.resource.RxPackagedResourceInventory;
import hiconic.rx.module.api.resource.RxPackagedResourceResolver;
import com.braintribe.model.resource.source.PackagedSource;

import hiconic.rx.platform.processing.resource.RxResourcesBuilding.RxUrlResourcesBuilder;

/** Index-backed resolver. Computed metadata is cached; mutable resource entities and payload streams are not. */
public class RxIndexedPackagedResourceResolver implements RxPackagedResourceResolver, PackagedResourceResolver {

	private final ClasspathIndex classpathIndex;
	private final String classpathRoot;
	private final Map<String, IndexedResource> resources;
	private final Map<ArtifactPathKey, CachedResource> resourcesByArtifactPath;
	private final RxPackagedResourceInventory inventory;

	public RxIndexedPackagedResourceResolver(ClasspathIndex classpathIndex, String classpathRoot) {
		this.classpathIndex = classpathIndex;
		this.classpathRoot = requireRoot(classpathRoot);
		this.resources = indexResources(classpathIndex);
		this.resourcesByArtifactPath = indexByArtifactPath(classpathIndex);
		this.inventory = new Inventory(resources.keySet());
	}

	/**
	 * A resolver for a folder below this one, for a module that owns such a folder and wants to address its files by a short path.
	 * <p>
	 * The platform knows nothing about what a folder means. A module that puts files there gives them their meaning, for example by serving them.
	 */
	@Override
	public RxPackagedResourceResolver below(String folder) {
		return new RxIndexedPackagedResourceResolver(classpathIndex, classpathRoot + normalizeDirectoryPath(folder) + "/");
	}

	@Override
	public RxPackagedResourceBuilder resource(String relativePath) {
		String path = normalizeResourcePath(relativePath);
		IndexedResource resource = resources.get(path);
		if (resource == null)
			throw new IllegalArgumentException("No indexed packaged resource found at: " + classpathRoot + path);

		// The artifact and the full artifact relative path, so that the produced source says where the file is without any further context.
		return new Builder(resource.artifactRelativePath, resource.artifact, resource.cachedResource);
	}

	@Override
	public RxPackagedResourceBuilder resource(String artifact, String artifactRelativePath) {
		String normalizedArtifact = normalizeArtifact(artifact);
		String normalizedPath = normalizeResourcePath(artifactRelativePath);
		CachedResource resource = resourcesByArtifactPath.get(new ArtifactPathKey(normalizedArtifact, normalizedPath));
		if (resource == null)
			throw new IllegalArgumentException("No indexed packaged resource found at " + normalizedArtifact + ":" + normalizedPath);
		return new Builder(normalizedPath, normalizedArtifact, resource);
	}

	@Override
	public RxPackagedResourceInventory inventory() {
		return inventory;
	}

	@Override
	public Maybe<Resource> resolveResource(String artifact, String path) {
		try {
			return Maybe.complete(resource(artifact, path).asPersistableResource());
		} catch (IllegalArgumentException e) {
			return NotFound.create(e.getMessage()).asMaybe();
		}
	}

	@Override
	public Maybe<com.braintribe.model.resource.source.PackagedSource> resolveSource(String artifact, String path) {
		try {
			return Maybe.complete(resource(artifact, path).asSource());
		} catch (IllegalArgumentException e) {
			return NotFound.create(e.getMessage()).asMaybe();
		}
	}

	@Override
	public Maybe<java.io.InputStream> openStream(String artifact, String path) {
		try {
			return Maybe.complete(resource(artifact, path).asHandle().asStream());
		} catch (IllegalArgumentException e) {
			return NotFound.create(e.getMessage()).asMaybe();
		}
	}

	private Map<String, IndexedResource> indexResources(ClasspathIndex classpathIndex) {
		Map<String, IndexedResource> result = new LinkedHashMap<>();
		for (ClasspathEntry entry : classpathIndex.forPrefix(classpathRoot)) {
			String artifactRelativePath = normalizeResourcePath(entry.path);
			String path = normalizeResourcePath(entry.path.substring(classpathRoot.length()));

			IndexedResource indexed = new IndexedResource(entry.origin, artifactRelativePath, new CachedResource(artifactRelativePath, entry.url));
			IndexedResource previous = result.putIfAbsent(path, indexed);
			if (previous != null && !previous.cachedResource.url.equals(entry.url))
				throw new IllegalStateException("Duplicate packaged resource path '" + path + "' below " + classpathRoot + ": "
						+ previous.cachedResource.url + " and " + entry.url);
		}
		return Map.copyOf(result);
	}

	private Map<ArtifactPathKey, CachedResource> indexByArtifactPath(ClasspathIndex classpathIndex) {
		Map<ArtifactPathKey, CachedResource> result = new LinkedHashMap<>();
		for (ClasspathEntry entry : classpathIndex.forPrefix("")) {
			if (entry.origin == null || entry.origin.isBlank())
				continue;
			String path = normalizeResourcePath(entry.path);
			ArtifactPathKey key = new ArtifactPathKey(normalizeArtifact(entry.origin), path);
			CachedResource previous = result.putIfAbsent(key, new CachedResource(path, entry.url));
			if (previous != null && !previous.url.equals(entry.url))
				throw new IllegalStateException("Duplicate indexed packaged resource '" + key + "': " + previous.url + " and " + entry.url);
		}
		return Map.copyOf(result);
	}

	private static String normalizeArtifact(String artifact) {
		if (artifact == null || artifact.isBlank())
			throw new IllegalArgumentException("A packaged resource artifact must not be empty");
		if (artifact.indexOf('/') >= 0 || artifact.indexOf('\\') >= 0 || artifact.equals(".") || artifact.equals(".."))
			throw new IllegalArgumentException("Invalid packaged resource artifact: " + artifact);
		return artifact;
	}

	private static String requireRoot(String root) {
		if (root == null || root.isBlank())
			throw new IllegalArgumentException("Classpath root must not be empty");
		return root.endsWith("/") ? root : root + "/";
	}

	public static String normalizeResourcePath(String path) {
		String normalized = normalize(path, false);
		if (normalized.isEmpty())
			throw new IllegalArgumentException("A packaged resource path must not be empty");
		return normalized;
	}

	public static String normalizeDirectoryPath(String path) {
		return normalize(path, true);
	}

	private static String normalize(String path, boolean directory) {
		if (path == null)
			throw new NullPointerException("path");
		String normalized = path.replace('\\', '/');
		while (normalized.startsWith("/"))
			normalized = normalized.substring(1);
		while (directory && normalized.endsWith("/") && !normalized.isEmpty())
			normalized = normalized.substring(0, normalized.length() - 1);
		for (String element : normalized.split("/", -1))
			if (element.equals(".") || element.equals("..") || (!directory && element.isEmpty()))
				throw new IllegalArgumentException("Invalid packaged resource path: " + path);
		if (directory && normalized.contains("//"))
			throw new IllegalArgumentException("Invalid packaged resource directory: " + path);
		return normalized;
	}

	private enum Enrichment {
		mimeType, fileSize, md5, specification
	}

	/** An entry of the root relative index: the file, plus where it really is. */
	private static final class IndexedResource {
		private final String artifact;
		private final String artifactRelativePath;
		private final CachedResource cachedResource;

		private IndexedResource(String artifact, String artifactRelativePath, CachedResource cachedResource) {
			this.artifact = artifact;
			this.artifactRelativePath = artifactRelativePath;
			this.cachedResource = cachedResource;
		}
	}

	private static class Builder implements RxPackagedResourceBuilder {
		private final String path;
		private final String artifact;
		private final CachedResource cachedResource;
		private final EnumSet<Enrichment> enrichments = EnumSet.noneOf(Enrichment.class);

		Builder(String path, String artifact, CachedResource cachedResource) {
			this.path = path;
			this.artifact = artifact;
			this.cachedResource = cachedResource;
		}

		@Override public String path() { return path; }
		@Override public ResourceHandle asHandle() { return cachedResource.handle; }
		@Override public RxPackagedResourceBuilder withMimeType() { enrichments.add(Enrichment.mimeType); return this; }
		@Override public RxPackagedResourceBuilder withFileSize() { enrichments.add(Enrichment.fileSize); return this; }
		@Override public RxPackagedResourceBuilder withMd5() { enrichments.add(Enrichment.md5); return this; }
		@Override public RxPackagedResourceBuilder withSpecification() { enrichments.add(Enrichment.specification); return this; }

		@Override
		public Resource asResource() {
			return cachedResource.asResource(enrichments);
		}

		@Override
		public Resource asPersistableResource() {
			return cachedResource.asPersistableResource(enrichments, asSource());
		}

		@Override
		public PackagedSource asSource() {
			PackagedSource result = PackagedSource.T.create();
			result.setPath(path);
			result.setArtifact(artifact);
			return result;
		}
	}

	private static final class ArtifactPathKey {
		private final String artifact;
		private final String path;

		private ArtifactPathKey(String artifact, String path) {
			this.artifact = artifact;
			this.path = path;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ArtifactPathKey))
				return false;
			ArtifactPathKey other = (ArtifactPathKey) obj;
			return artifact.equals(other.artifact) && path.equals(other.path);
		}

		@Override
		public int hashCode() {
			return 31 * artifact.hashCode() + path.hashCode();
		}

		@Override
		public String toString() {
			return artifact + ":" + path;
		}
	}

	private static class CachedResource {
		private final String path;
		private final URL url;
		private final ResourceHandle handle;
		private final EnumSet<Enrichment> completed = EnumSet.noneOf(Enrichment.class);
		private String mimeType;
		private Long fileSize;
		private String md5;
		private RasterImageSpecification specification;

		CachedResource(String path, URL url) {
			this.path = path;
			this.url = url;
			this.handle = new RxUrlResourcesBuilder(url);
		}

		synchronized Resource asResource(Set<Enrichment> requested) {
			ensureMetadata(requested);
			Resource result = Resource.createTransient(handle::asStream);
			applyMetadata(result, requested);
			return result;
		}

		synchronized Resource asPersistableResource(Set<Enrichment> requested, PackagedSource source) {
			ensureMetadata(requested);
			Resource result = Resource.T.create();
			result.setResourceSource(source);
			applyMetadata(result, requested);
			return result;
		}

		private void ensureMetadata(Set<Enrichment> requested) {
			if (requested.contains(Enrichment.mimeType) && !completed.contains(Enrichment.mimeType)) {
				mimeType = PlatformMimeTypeDetector.instance.getMimeType((File) null, fileName(path));
				completed.add(Enrichment.mimeType);
			}
			if (requested.contains(Enrichment.fileSize) && !completed.contains(Enrichment.fileSize)) {
				fileSize = contentLength();
				completed.add(Enrichment.fileSize);
			}
			if (requested.contains(Enrichment.md5) && !completed.contains(Enrichment.md5)) {
				md5 = computeMd5();
				completed.add(Enrichment.md5);
			}
			if (requested.contains(Enrichment.specification) && !completed.contains(Enrichment.specification)) {
				specification = imageSpecification();
				completed.add(Enrichment.specification);
			}
		}

		private void applyMetadata(Resource result, Set<Enrichment> requested) {
			result.setName(fileName(path));
			if (requested.contains(Enrichment.mimeType))
				result.setMimeType(mimeType);
			if (requested.contains(Enrichment.fileSize))
				result.setFileSize(fileSize);
			if (requested.contains(Enrichment.md5))
				result.setMd5(md5);
			if (requested.contains(Enrichment.specification))
				result.setSpecification(specification);
		}

		private Long contentLength() {
			try {
				long length = url.openConnection().getContentLengthLong();
				return length < 0 ? null : length;
			} catch (IOException e) {
				throw new UncheckedIOException("Could not determine packaged resource size: " + path, e);
			}
		}

		private String computeMd5() {
			try {
				MessageDigest digest = MessageDigest.getInstance("MD5");
				try (InputStream in = new DigestInputStream(new BufferedInputStream(handle.asStream()), digest)) {
					IOTools.consume(in);
				}
				return StringTools.toHex(digest.digest());
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("MD5 algorithm unavailable", e);
			} catch (IOException e) {
				throw new UncheckedIOException("Could not hash packaged resource: " + path, e);
			}
		}

		private RasterImageSpecification imageSpecification() {
			try (InputStream in = new BufferedInputStream(handle.asStream()); ImageInputStream imageIn = ImageIO.createImageInputStream(in)) {
				if (imageIn == null)
					return null;
				var readers = ImageIO.getImageReaders(imageIn);
				if (!readers.hasNext())
					return null;
				ImageReader reader = readers.next();
				try {
					reader.setInput(imageIn);
					RasterImageSpecification result = RasterImageSpecification.T.create();
					result.setPixelWidth(reader.getWidth(0));
					result.setPixelHeight(reader.getHeight(0));
					result.setPageCount(reader.getNumImages(true));
					return result;
				} finally {
					reader.dispose();
				}
			} catch (IOException e) {
				throw new UncheckedIOException("Could not inspect packaged image resource: " + path, e);
			}
		}
	}

	private static class Inventory implements RxPackagedResourceInventory {
		private final Set<String> paths;

		Inventory(Set<String> paths) {
			this.paths = Set.copyOf(paths);
		}

		@Override public Set<String> resourcePaths() { return paths; }

		@Override
		public boolean contains(String relativePath) {
			try {
				return paths.contains(normalizeResourcePath(relativePath));
			} catch (IllegalArgumentException | NullPointerException e) {
				return false;
			}
		}

		@Override
		public List<RxPackagedResourceEntry> list(String relativeDirectory) {
			String directory = normalizeDirectoryPath(relativeDirectory);
			String prefix = directory.isEmpty() ? "" : directory + "/";
			Map<String, RxPackagedResourceEntry> entries = new LinkedHashMap<>();
			for (String path : paths) {
				if (!path.startsWith(prefix))
					continue;
				String remainder = path.substring(prefix.length());
				int separator = remainder.indexOf('/');
				if (separator < 0)
					entries.put(remainder, new RxPackagedResourceEntry(path, remainder, false));
				else {
					String name = remainder.substring(0, separator);
					entries.putIfAbsent(name, new RxPackagedResourceEntry(prefix + name + "/", name, true));
				}
			}
			List<RxPackagedResourceEntry> result = new ArrayList<>(entries.values());
			result.sort(Comparator.comparing(RxPackagedResourceEntry::name));
			return List.copyOf(result);
		}
	}

	private static String fileName(String path) {
		int separator = path.lastIndexOf('/');
		return separator < 0 ? path : path.substring(separator + 1);
	}
}
