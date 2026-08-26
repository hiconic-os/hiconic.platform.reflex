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
package hiconic.rx.test.common;

import java.util.Map;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;

import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.RxPlatform;
import hiconic.rx.platform.conf.RxProperties;
import hiconic.rx.platform.conf.SystemProperties;

public abstract class AbstractRxTest {

	protected RxPlatform platform;
	protected RxPlatformContract platformContract;
	protected Evaluator<ServiceRequest> evaluator;

	@Before
	public void onBefore() {
		try {
			platform = new RxPlatform(new String[] {}, systemPropertyLookup("res/app"),
					applicationPropertyLookup(AbstractRxTest.this.getClass().getSimpleName()), managedPropertyOverrides());
			platformContract = platform.getContract();
			evaluator = platform.getContract().serviceProcessing().evaluator();

		} catch (UnsatisfiedMaybeTunneling e) {
			System.err.print(e.getMaybe().whyUnsatisfied().stringify());
			throw e;
		}
	}

	public static RxPlatform loadPlatform(String appName) {
		return loadPlatform("res/app", appName);
	}

	public static RxPlatform loadPlatform(String appDir, String appName) {
		return new RxPlatform(systemPropertyLookup(appDir), applicationPropertyLookup(appName));
	}

	public static RxPlatform loadPlatform(String appDir, String appName, Map<String, String> managedPropertyOverrides) {
		return new RxPlatform(new String[] {}, systemPropertyLookup(appDir), applicationPropertyLookup(appName), managedPropertyOverrides);
	}

	@After
	public void onAfter() throws Exception {
		if (platform == null)
			return;

		try {
			platform.close();
		} finally {
			afterPlatformClosed();
		}
	}

	//
	// Utils
	//

	protected <C extends RxExportContract> C resolveExportContract(Class<C> contractClass) {
		return platform.getWireContext().contract(contractClass);
	}

	protected Map<String, String> managedPropertyOverrides() {
		return Map.of();
	}

	/**
	 * Hook for fixtures which reuse external resources between otherwise isolated
	 * platform starts. It runs only after a successfully created platform has been
	 * closed, so persistent test state can safely be reset before the next start.
	 */
	protected void afterPlatformClosed() throws Exception {
	}

	//
	// Internal
	//

	private static Function<String, String> systemPropertyLookup(String appDir) {
		return RxProperties.overrideLookup( //
				RxPlatform.defaultSystemPropertyLookup(), //
				n -> {
					switch (n) {
						case SystemProperties.PROPERTY_APP_DIR:
							return appDir;
						default:
							return null;
					}
				});
	}

	private static Function<String, String> applicationPropertyLookup(String appName) {
		return RxProperties.overrideLookup( //
				RxPlatform.defaultApplicationPropertyLookup(), //
				n -> {
					switch (n) {
						case "applicationName":
							return appName;
						default:
							return null;
					}
				} //
		);
	}

}
