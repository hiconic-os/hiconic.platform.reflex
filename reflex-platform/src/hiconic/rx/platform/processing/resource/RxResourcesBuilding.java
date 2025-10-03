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
package hiconic.rx.platform.processing.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import com.braintribe.codec.marshaller.api.MarshallException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.common.lcd.function.CheckedFunction;
import com.braintribe.model.resource.api.ResourceHandle;
import com.braintribe.utils.IOTools;


/**
 * @author peter.gazdik
 */
public class RxResourcesBuilding {

	public static class RxPathResourcesBuilder extends AbstractResourcesBuilder {

		private final Path path;

		public static RxPathResourcesBuilder create(Path base, String relativePath) throws InvalidPathException {
			relativePath = sanitizeRelativePath(relativePath);
			Path path = base.resolve(relativePath);
			return new RxPathResourcesBuilder(path);
		}

		public static RxPathResourcesBuilder create(String absolutePath) throws InvalidPathException {
			Path path = Paths.get(absolutePath);
			return new RxPathResourcesBuilder(path);
		}

		public RxPathResourcesBuilder(Path path) {
			this.path = path;
		}
		
		@Override
		public URL asUrl() {
			try {
				return this.path.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public Path asPath() {
			return this.path;
		}

		@Override
		public File asFile() {
			return this.path.toFile();
		}

		public <T> T asAssembly(Marshaller marshaller, T defaultValue) {
			if (Files.exists(path))
				return asAssembly(marshaller);
			else
				return defaultValue;
		}
		
		@Override
		public InputStream asStream() {
			try {
				return Files.newInputStream(this.path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		private static String sanitizeRelativePath(String relativePath) {
			if (relativePath == null || relativePath.equals("") || relativePath.equals("/")) {
				return ".";
			} else if (relativePath.startsWith("/")) {
				return relativePath.substring(1);
			}
			return relativePath;
		}

	}

	public static class RxClasspathResourcesBuilder extends AbstractResourcesBuilder {

		private final String originalPath;
		private URI uri;
		private final ClassLoader classLoader;

		public static RxClasspathResourcesBuilder create(String value) {
			return new RxClasspathResourcesBuilder(value, RxClasspathResourcesBuilder.class.getClassLoader());
		}

		public static RxClasspathResourcesBuilder create(String value, ClassLoader classLoader) {
			return new RxClasspathResourcesBuilder(value, classLoader);
		}

		public RxClasspathResourcesBuilder(String originalPath, ClassLoader classLoader) {
			this.classLoader = classLoader;
			this.originalPath = sanitizePath(originalPath);
		}

		private URI getUri() throws IOException {
			URI uri = findUri();
			if (uri == null)
				throw new IOException("Classpath resource no found: " + originalPath);

			return uri;
		}

		private URI findUri() throws IOException {
			if (uri == null) {
				URL url = classLoader.getResource(originalPath);
				if (url == null)
					return null;

				try {
					uri = url.toURI();
				} catch (URISyntaxException e) {
					throw new IOException("Malformed URI: " + originalPath, e);
				}
			}

			return uri;

		}

		@Override
		public URL asUrl() {
			try {
				return getUri().toURL();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public Path asPath() {
			try {
				URI pathUri = getUri();

				Path path = uriToPath(pathUri);
				if (path == null)
					throw new IOException("No path could be resolved from [ " + pathUri + " ]");

				return path;

			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public File asFile() {
			Path path = asPath();
			try {
				return path.toFile();
			} catch (Exception e) {
				throw new UnsupportedOperationException("Cannot represent [ " + (uri != null ? uri : path) + " ] as a java.io.File", e);
			}
		}

		public <T> T asAssembly(Marshaller marshaller, T defaultValue) {
			if (uriExists())
				return asAssembly(marshaller);
			else
				return defaultValue;
		}

		private boolean uriExists() {
			try {
				return findUri() != null;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public InputStream asStream() {
			try {
				return asUrl().openStream();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private static String sanitizePath(String relativePath) {
			if (relativePath == null || relativePath.equals("") || relativePath.equals("/"))
				return "";
			else if (relativePath.startsWith("/"))
				return relativePath.substring(1);
			else
				return relativePath;
		}

	}

	private static abstract class AbstractResourcesBuilder implements ResourceHandle {

		@Override
		public String asString(String encoding) {
			return processStream(stream -> IOTools.slurp(stream, encoding));
		}

		public <T> T asAssembly(Marshaller marshaller) throws MarshallException {
			return (T) processStream(marshaller::unmarshall);
		}

		@Override
		public Properties asProperties() {
			return processStream(IOTools::readAsProperties);
		}
		
		protected <T> T processStream(CheckedFunction<InputStream, T, IOException>  processor) {
			try (InputStream stream = asStream()) {
				return processor.apply(stream);

			} catch (IOException e) {
				throw new UncheckedIOException(resourceErrorMessage(), e);

			} catch (Exception e) {
				throw new MarshallException(resourceErrorMessage(), e);
			}
		}

		protected String resourceErrorMessage() {
			try {
				return "Error while processing resource with URL: " + asUrl();
			} catch (Exception e) {
				return "Error while processing resource whose URL cannot be resolved due to: " + e.getMessage();
			}
		}
	}

	public static Path uriToPath(URI uri) throws IOException {
		try {
			return Paths.get(uri);

		} catch (FileSystemNotFoundException notFoundEx) {

			try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.<String, Object> emptyMap())) {
				return fs.provider().getPath(uri);

			} catch (RuntimeException | IOException e) {
				e.addSuppressed(notFoundEx);
				throw e;
			}

		}

	}
}
