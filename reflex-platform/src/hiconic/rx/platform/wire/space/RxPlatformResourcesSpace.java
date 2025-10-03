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
package hiconic.rx.platform.wire.space;

import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.module.api.wire.RxResourcesBuilder;
import hiconic.rx.platform.processing.resource.RxResourcesBuilding.RxPathResourcesBuilder;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

/**
 * 
 */
@Managed
public class RxPlatformResourcesSpace implements RxPlatformResourcesContract {

	@Import
	private RxPlatformConfigContract config;

	@Override
	public RxResourcesBuilder resource(String path) {
		return PathResourcesBuilder.create(appPath(), resolve(path));
	}

	@Override
	public RxResourcesBuilder tmp(String path) {
		return PathResourcesBuilder.create(tmpPath(), resolve(path));
	}

	@Override
	public RxResourcesBuilder cache(String path) {
		return PathResourcesBuilder.create(cachePath(), resolve(path));
	}

	@Override
	public RxResourcesBuilder data(String path) {
		return PathResourcesBuilder.create(dataPath(), resolve(path));
	}

	@Override
	public RxResourcesBuilder conf(String path) throws UncheckedIOException {
		return PathResourcesBuilder.create(confPath(), resolve(path));
	}

	@Override
	@Managed
	public Path tmpPath() {
		return resolvePath(appPath(), "tmp");
	}

	@Override
	@Managed
	public Path cachePath() {
		return resolvePath(appPath(), "cache");
	}

	@Override
	@Managed
	public Path dataPath() {
		return resolvePath(appPath(), "data");
	}

	@Override
	@Managed
	public Path confPath() {
		return resolvePath(appPath(), "conf");
	}

	@Override
	public Path rootPath() {
		return appPath();
	}

	@Managed
	private Path appPath() {
		return config.appDir().toPath();
	}

	/** Resolves possible place-holders in the provided String. */
	private String resolve(String path) {
		NullSafe.nonNull(path, "path");

		// String resolvedPath = environment.resolve(path);

		return path;

	}

	private Path resolvePath(Path defaultParent, String defaultPath) {
		return defaultParent.resolve(defaultPath);
	}

	public static class PathResourcesBuilder extends RxPathResourcesBuilder implements RxResourcesBuilder {

		public static PathResourcesBuilder create(Path base, String relativePath) throws InvalidPathException {
			relativePath = sanitizeRelativePath(relativePath);
			Path path = base.resolve(relativePath);
			return new PathResourcesBuilder(path);
		}

		public static PathResourcesBuilder create(String absolutePath) throws InvalidPathException {
			Path path = Paths.get(absolutePath);
			return new PathResourcesBuilder(path);
		}

		private PathResourcesBuilder(Path path) {
			super(path);
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

}
