// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.platform.loading;

import java.util.List;
import java.util.Map;
import java.util.Set;

import hiconic.rx.module.api.wire.RxModule;

/* package */ record LoadedRxModules(List<RxModule<?>> modules, Map<RxModule<?>, Set<RxModule<?>>> declaredDependencies) {
}
