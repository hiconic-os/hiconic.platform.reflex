package hiconic.rx.hello.wire;

import java.util.List;

import com.braintribe.wire.api.module.WireModule;

import hiconic.rx.hello.wire.space.ReflexHelloAppRxModuleSpace;
import hiconic.rx.module.api.wire.RxModule;

public enum ReflexHelloAppRxModule implements RxModule<ReflexHelloAppRxModuleSpace> {
	INSTANCE;
	
	@Override
	public List<WireModule> dependencies() {
		// add dependencies to other wire modules if required
		return List.of();
	}
	
	
}
