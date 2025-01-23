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

import java.util.function.Function;

import org.junit.After;
import org.junit.Before;

import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.platform.RxPlatform;
import hiconic.rx.platform.conf.RxProperties;
import hiconic.rx.platform.conf.SystemProperties;

public abstract class AbstractRxTest {
	protected RxPlatform platform;
	protected Evaluator<ServiceRequest> evaluator;
	
	private Function<String, String> systemPropertyLookup() {
		return RxProperties.overrideLookup( //
			RxPlatform.defaultSystemPropertyLookup(), // 
			n -> {
				switch (n) {
				case SystemProperties.PROPERTY_APP_DIR: return "res/app";
				default: return null;
				}
			}
		);
	}
	
	private Function<String, String> applicationPropertyLookup() {
		return RxProperties.overrideLookup( //
			RxPlatform.defaultApplicationPropertyLookup(), 
			n -> {
				switch (n) {
				case "applicationName": return AbstractRxTest.this.getClass().getSimpleName();
				default: return null;
				}
			}
		);
	}
	
	@Before
	public void onBefore() {
		try {
			platform = new RxPlatform(systemPropertyLookup(), applicationPropertyLookup());
			evaluator = platform.getContract().evaluator();
		} catch (UnsatisfiedMaybeTunneling e) {
			System.err.print(e.getMaybe().whyUnsatisfied().stringify());
			throw e;
		}
	}
	
	@After
	public void onAfter() {
		if (platform != null)
			platform.close();
	}
}
