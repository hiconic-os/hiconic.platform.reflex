package hiconic.rx.explorer.processing.platformreflection.application;

import java.util.function.Supplier;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.reflection.model.application.RxAppInfo;

/**
 * @author peter.gazdik
 */
public class StandardRxAppInfoProvider implements Supplier<RxAppInfo> {

	private RxPlatformContract platform;

	public void setPlatformContract(RxPlatformContract platform) {
		this.platform = platform;
	}

	@Override
	public RxAppInfo get() {
		RxAppInfo result = RxAppInfo.T.create();
		result.setApplicationName(platform.applicationName());
		result.setApplicationId(platform.applicationId());

		return result;
	}

}
