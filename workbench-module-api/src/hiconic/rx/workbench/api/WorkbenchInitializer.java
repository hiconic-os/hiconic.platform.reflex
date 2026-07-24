// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.api;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

@FunctionalInterface
public interface WorkbenchInitializer {

	void initialize(PersistenceGmSession session);

}
