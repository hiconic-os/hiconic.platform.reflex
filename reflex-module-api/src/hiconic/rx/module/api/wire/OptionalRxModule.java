// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.module.api.wire;

import java.util.List;

/** An RX module activated when required or when its activation dependencies are active. */
public record OptionalRxModule(Class<? extends RxModule<?>> module,
		List<Class<? extends RxModule<?>>> activationDependencies) {

	public OptionalRxModule {
		activationDependencies = List.copyOf(activationDependencies);
	}
}
