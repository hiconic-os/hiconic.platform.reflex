// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.api;

import java.util.Objects;

/** A workbench initializer together with the stable identity of its managed Wire bean. */
public record WorkbenchInitializerRegistration(WorkbenchInitializer initializer, WorkbenchInitializerIdentity identity) {

	public WorkbenchInitializerRegistration {
		Objects.requireNonNull(initializer, "initializer");
		Objects.requireNonNull(identity, "identity");
	}

}
