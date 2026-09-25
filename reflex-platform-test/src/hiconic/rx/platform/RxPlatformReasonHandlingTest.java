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
package hiconic.rx.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.gm.model.reason.essential.IoError;

public class RxPlatformReasonHandlingTest {

	@Test
	public void extractsReasonWrappedByFrameworkException() {
		Reason reason = IoError.create("listener address already in use");
		RuntimeException frameworkException = new RuntimeException("Error while constructing bean", new ReasonException(reason));

		assertThat(RxPlatform.reasonFrom(frameworkException)).isSameAs(reason);
	}

	@Test
	public void leavesUnexpectedExceptionUnclassified() {
		assertThat(RxPlatform.reasonFrom(new IllegalStateException("unexpected"))).isNull();
	}
}
