package hiconic.rx.messaging.module.wire;

import hiconic.rx.messaging.api.MessagingBaseContract;
import hiconic.rx.messaging.api.MessagingDestinationsContract;
import hiconic.rx.messaging.module.wire.space.MessagingBaseRxModuleSpace;
import hiconic.rx.messaging.module.wire.space.MessagingDestinationsSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum MessagingBaseRxModule implements RxModule<MessagingBaseRxModuleSpace> {

	INSTANCE;

	
	@SuppressWarnings("deprecation")
	@Override
	public void bindExports(Exports exports) {
		exports.bind(MessagingBaseContract.class, MessagingBaseRxModuleSpace.class);
		exports.bind(MessagingDestinationsContract.class, MessagingDestinationsSpace.class);
	}
}