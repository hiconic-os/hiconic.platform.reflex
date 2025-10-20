// ============================================================================
package hiconic.rx.reflection.model.jvm;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;

import hiconic.rx.reflection.model.api.PlatformReflectionResponse;

public interface HeapDump extends PlatformReflectionResponse {

	EntityType<HeapDump> T = EntityTypes.T(HeapDump.class);

	String heapDump = "heapDump";

	void setHeapDump(Resource heapDump);
	Resource getHeapDump();

}
