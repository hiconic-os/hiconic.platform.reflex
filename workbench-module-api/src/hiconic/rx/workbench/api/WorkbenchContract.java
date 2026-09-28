// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.api;

import hiconic.rx.module.api.wire.RxExportContract;

public interface WorkbenchContract extends RxExportContract {

	/**
	 * Associates a data access with its workbench access. Re-registering the same association is harmless; assigning a
	 * different workbench to an already associated data access is rejected.
	 */
	void associateWorkbench(String dataAccessId, String workbenchAccessId);

	/** Returns the workbench access associated with the given data access, or {@code null} if none was registered. */
	String workbenchAccessId(String dataAccessId);

	/**
	 * Registers an initializer for a configured workbench access. Initializers are applied in registration order after all modules were deployed.
	 * A standard workbench skeleton is ensured before custom initializers run.
	 */
	void registerInitializer(String workbenchAccessId, WorkbenchInitializer initializer);

	/** Associates the accesses and registers the initializer as one coherent contribution. */
	default void registerWorkbench(String dataAccessId, String workbenchAccessId, WorkbenchInitializer initializer) {
		associateWorkbench(dataAccessId, workbenchAccessId);
		registerInitializer(workbenchAccessId, initializer);
	}

	/** Registers an initializer with a stable Wire-derived identity for all entities it creates. */
	default void registerInitializer(String workbenchAccessId, WorkbenchInitializerRegistration registration) {
		registerInitializer(workbenchAccessId, registration.initializer());
	}

	/** Associates the accesses and registers the initializer as one coherent contribution. */
	default void registerWorkbench(String dataAccessId, String workbenchAccessId, WorkbenchInitializerRegistration registration) {
		associateWorkbench(dataAccessId, workbenchAccessId);
		registerInitializer(workbenchAccessId, registration);
	}

}
