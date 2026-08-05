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
package hiconic.rx.platform.conf;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

import com.braintribe.mimetype.MimeTypeDetector;

/**
 * Stable {@link MimeTypeDetector} facade whose delegate may be contributed later during RX platform configuration.
 * <p>
 * Model configuration happens before the platform configuration phase. Components created while models are configured
 * must therefore retain this facade rather than the detector which happened to be configured at that time.
 */
public final class DelegatingMimeTypeDetector implements MimeTypeDetector {

	private final Supplier<? extends MimeTypeDetector> delegateSupplier;

	public DelegatingMimeTypeDetector(Supplier<? extends MimeTypeDetector> delegateSupplier) {
		this.delegateSupplier = requireNonNull(delegateSupplier, "delegateSupplier");
	}

	@Override
	public String getMimeType(File file, String fileName) throws RuntimeException {
		return delegate().getMimeType(file, fileName);
	}

	@Override
	public String getMimeType(InputStream inputStream, String fileName) {
		return delegate().getMimeType(inputStream, fileName);
	}

	private MimeTypeDetector delegate() {
		return requireNonNull(delegateSupplier.get(), "No MIME type detector configured");
	}
}
