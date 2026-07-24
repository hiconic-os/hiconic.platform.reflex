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
import com.braintribe.mimetype.PlatformMimeTypeDetector;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.api.ResourceHandle;
import com.braintribe.model.resource.specification.RasterImageSpecification;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;

import hiconic.rx.module.api.resource.RxPackagedResourceBuilder;
import hiconic.rx.module.api.resource.RxPackagedResourceEntry;
import hiconic.rx.module.api.resource.RxPackagedResourceInventory;
import hiconic.rx.module.api.resource.RxPackagedResourceResolver;
import hiconic.rx.platform.processing.resource.RxResourcesBuilding.RxUrlResourcesBuilder;
import hiconic.rx.resource.model.packaged.PackagedResourceNamespace;
import hiconic.rx.resource.model.packaged.PackagedResourceSource;

/** Index-backed resolver. Computed metadata is cached; mutable resource entities and payload streams are not. */
public class RxIndexedPackagedResourceResolver implements RxPackagedResourceResolver {

	private final String classpathRoot;
	private final PackagedResourceNamespace namespace;
	private final Map<String, CachedResource> resources;
	private final RxPackagedResourceInventory inventory;

	public RxIndexedPackagedResourceResolver(ClasspathIndex classpathIndex, String classpathRoot, PackagedResourceNamespace namespace) {
		this.classpathRoot = requireRoot(classpathRoot);
		this.namespace = namespace;
		this.resources = indexResources(classpathIndex);
		this.inventory = new Inventory(resources.keySet());
	}

	@Override
	public RxPackagedResourceBuilder resource(String relativePath) {
		String path = normalizeResourcePath(relativePath);
		CachedResource resource = resources.get(path);
		if (resource == null)
			throw new IllegalArgumentException("No indexed packaged resource found at: " + path);
		return new Builder(path, resource, namespace);
	}

	@Override
	public RxPackagedResourceInventory inventory() {
		return inventory;
	}

	private Map<String, CachedResource> indexResources(ClasspathIndex classpathIndex) {
		Map<String, CachedResource> result = new LinkedHashMap<>();
		for (ClasspathEntry entry : classpathIndex.forPrefix(classpathRoot)) {
			String path = normalizeResourcePath(entry.path.substring(classpathRoot.length()));
			CachedResource previous = result.putIfAbsent(path, new CachedResource(path, entry.url));
			if (previous != null && !previous.url.equals(entry.url))
				throw new IllegalStateException("Duplicate packaged resource path '" + path + "' below " + classpathRoot + ": "
						+ previous.url + " and " + entry.url);
		}
		return Map.copyOf(result);
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

	private static class Builder implements RxPackagedResourceBuilder {
		private final String path;
		private final CachedResource cachedResource;
		private final PackagedResourceNamespace namespace;
		private final EnumSet<Enrichment> enrichments = EnumSet.noneOf(Enrichment.class);

		Builder(String path, CachedResource cachedResource, PackagedResourceNamespace namespace) {
			this.path = path;
			this.cachedResource = cachedResource;
			this.namespace = namespace;
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
		public PackagedResourceSource asSource() {
			PackagedResourceSource result = PackagedResourceSource.T.create();
			result.setPath(path);
			result.setNamespace(namespace);
			return result;
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

		synchronized Resource asPersistableResource(Set<Enrichment> requested, PackagedResourceSource source) {
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
