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
package hiconic.rx.platform.thread;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

import com.braintribe.common.attribute.TypeSafeAttribute;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.test.common.AbstractRxTest;

public class AttributeContextThreadTransitionTest extends AbstractRxTest {
	private static final String MARKER = "calling-thread-context";

	public interface ThreadContextMarker extends TypeSafeAttribute<String> {
		// Test marker.
	}

	@Test
	public void platformExecutorTransfersAttributeContext() throws Exception {
		Future<String> result = withMarker(() -> platformContract.execution().executorService().submit(this::currentMarker));

		assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo(MARKER);
	}

	@Test
	public void platformSchedulerTransfersAttributeContext() throws Exception {
		Future<String> result = withMarker(
				() -> platformContract.execution().scheduledExecutorService().schedule(this::currentMarker, 1, TimeUnit.MILLISECONDS));

		assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo(MARKER);
	}

	private String currentMarker() {
		return AttributeContexts.peek().findOrNull(ThreadContextMarker.class);
	}

	private <T> T withMarker(Supplier<T> action) {
		return AttributeContexts.derivePeek().set(ThreadContextMarker.class, MARKER).buildAnd().execute(action::get);
	}
}
