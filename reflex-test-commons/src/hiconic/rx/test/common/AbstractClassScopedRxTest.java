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
package hiconic.rx.test.common;

import org.junit.After;
import org.junit.Before;

/** Base class used together with an explicitly declared {@link RxPlatformTestClassRule}. */
public abstract class AbstractClassScopedRxTest extends AbstractRxTest {

	protected abstract RxPlatformTestClassRule platformClassRule();

	@Override
	@Before
	public final void onBefore() {
		platform = platformClassRule().platform();
		platformContract = platform.getContract();
		evaluator = platformContract.serviceProcessing().evaluator();
	}

	@Override
	@After
	public final void onAfter() {
		// The class rule owns the platform lifecycle.
	}
}
