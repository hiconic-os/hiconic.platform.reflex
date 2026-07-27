package hiconic.rx.log.reflection.demo.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.demo.processing.DemoLogProducer;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class LogReflectionDemoRxModuleSpace implements RxModuleContract {
	private static final String ENABLED_EXPRESSION = "${LOG_REFLECTION_DEMO_ENABLED:-false}";
	private static final String INTERVAL_EXPRESSION = "${LOG_REFLECTION_DEMO_INTERVAL_MILLIS:-750}";

	@Import
	private RxPlatformContract platform;
	private boolean started;

	@Override
	public void onApplicationReady() {
		if (enabled()) {
			producer().start();
			started = true;
		}
	}

	@Override
	public void onApplicationShutdown() {
		if (started)
			producer().stop();
	}

	@Managed
	private DemoLogProducer producer() {
		DemoLogProducer bean = new DemoLogProducer();
		bean.setScheduler(platform.execution().scheduledExecutorService());
		bean.setIntervalMillis(intervalMillis());
		return bean;
	}

	private boolean enabled() {
		return Boolean.parseBoolean(resolve(ENABLED_EXPRESSION));
	}

	private long intervalMillis() {
		return Long.parseLong(resolve(INTERVAL_EXPRESSION));
	}

	private String resolve(String expression) {
		return platform.configuration().propertyResolver().resolve(expression);
	}
}
