// ============================================================================
package hiconic.rx.reflection.model.module;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface RxModuleInfo extends RxComponentInfo {

	EntityType<RxModuleInfo> T = EntityTypes.T(RxModuleInfo.class);

}
