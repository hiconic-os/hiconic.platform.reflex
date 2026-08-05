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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;

import org.junit.Test;

import com.braintribe.mimetype.MimeTypeDetector;
import com.braintribe.provider.Box;

public class DelegatingMimeTypeDetectorTest {

	@Test
	public void existingFacadeUsesDetectorContributedLater() {
		Box<MimeTypeDetector> delegate = Box.of(detector("initial/type"));
		MimeTypeDetector facade = new DelegatingMimeTypeDetector(() -> delegate.value);

		assertThat(facade.getMimeType((InputStream) null, "sample.bin")).isEqualTo("initial/type");

		delegate.value = detector("contributed/type");

		assertThat(facade.getMimeType((InputStream) null, "sample.bin")).isEqualTo("contributed/type");
		assertThat(facade.getMimeType((File) null, "sample.bin")).isEqualTo("contributed/type");
	}

	private static MimeTypeDetector detector(String mimeType) {
		return new MimeTypeDetector() {
			@Override
			public String getMimeType(File file, String fileName) {
				return mimeType;
			}

			@Override
			public String getMimeType(InputStream inputStream, String fileName) {
				return mimeType;
			}
		};
	}
}
