// ============================================================================
package hiconic.rx.reflection.model.api;


import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.reflection.model.application.RxAppInfo;
import hiconic.rx.reflection.model.system.SystemInfo;


public interface PlatformReflection extends PlatformReflectionResponse {
	
	EntityType<PlatformReflection> T = EntityTypes.T(PlatformReflection.class);
	
	SystemInfo getSystemInfo();
	void setSystemInfo(SystemInfo system);
	
	// No idea, old impl was specific to Tomcat
//	HostInfo getHost();
//	void setHost(HostInfo hostInfo);
	
	RxAppInfo getRxAppInfo();
	void setRxAppInfo(RxAppInfo rxAppInfo);
	
}
