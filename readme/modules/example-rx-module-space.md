```java
package example.module.wire.space;

import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class ExampleRxModuleSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;
	
	@Override
	public void addApiModels(ConfigurationModelBuilder builder) {
		// TODO: add api models to the platform
	}
	
	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		// TODO: register processors
	}
}
```
