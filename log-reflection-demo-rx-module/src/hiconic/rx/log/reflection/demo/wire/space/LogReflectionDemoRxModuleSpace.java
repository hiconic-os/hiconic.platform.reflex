package hiconic.rx.log.reflection.demo.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.demo.processing.DemoLogProducer;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class LogReflectionDemoRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public void onApplicationReady() {
		producer().start();
	}

	@Override
	public void onApplicationShutdown() {
		producer().stop();
	}

	@Managed
	private DemoLogProducer producer() {
		DemoLogProducer bean = new DemoLogProducer();
		bean.setScheduler(platform.execution().scheduledExecutorService());
		bean.setIntervalMillis(750);
		return bean;
	}
}
