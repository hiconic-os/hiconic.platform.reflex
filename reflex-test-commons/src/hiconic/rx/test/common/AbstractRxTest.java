package hiconic.rx.test.common;

import java.util.function.Function;

import org.junit.After;
import org.junit.Before;

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
		platform = new RxPlatform(systemPropertyLookup(), applicationPropertyLookup());
		evaluator = platform.getContract().evaluator();
	}
	
	@After
	public void onAfter() {
		if (platform != null)
			platform.close();
	}
}
