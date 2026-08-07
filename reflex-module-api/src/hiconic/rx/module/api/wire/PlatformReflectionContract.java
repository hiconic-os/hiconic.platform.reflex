// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.module.api.wire;

import java.util.List;

import com.braintribe.wire.api.space.WireSpace;

/** Platform-wide reflection of the RX modules loaded so far. */
public interface PlatformReflectionContract extends WireSpace {

	List<ModuleReflectionContract> modules();

}
