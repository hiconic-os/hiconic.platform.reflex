```java
package example.module.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import example.module.wire.space.ExampleRxModuleSpace;

import hiconic.rx.module.api.wire.RxModuleContract;

public enum ExampleRxModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, ExampleRxModuleSpace.class);
	}
}
```