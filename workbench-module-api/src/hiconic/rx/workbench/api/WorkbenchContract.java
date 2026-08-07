// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.api;

import hiconic.rx.module.api.wire.RxExportContract;

public interface WorkbenchContract extends RxExportContract {

	/**
	 * Registers an initializer for a configured workbench access. Initializers are applied in registration order after all modules were deployed.
	 * A standard workbench skeleton is ensured before custom initializers run.
	 */
	void registerInitializer(String workbenchAccessId, WorkbenchInitializer initializer);

	/** Registers an initializer with a stable Wire-derived identity for all entities it creates. */
	default void registerInitializer(String workbenchAccessId, WorkbenchInitializerRegistration registration) {
		registerInitializer(workbenchAccessId, registration.initializer());
	}

}
