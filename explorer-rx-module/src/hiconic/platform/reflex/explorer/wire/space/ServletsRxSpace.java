// ============================================================================
package hiconic.platform.reflex.explorer.wire.space;

import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import dev.hiconic.servlet.impl.remote.StandardRemoteClientAddressResolver;

@Managed
public class ServletsRxSpace implements WireSpace {

	@Managed
	public RemoteClientAddressResolver remoteAddressResolver() {
		StandardRemoteClientAddressResolver resolver = new StandardRemoteClientAddressResolver();
		resolver.setIncludeForwarded(true);
		resolver.setIncludeXForwardedFor(true);
		resolver.setIncludeXRealIp(true);
		resolver.setLenientParsing(true);
		return resolver;
	}

}
