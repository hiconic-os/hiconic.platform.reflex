package hiconic.platform.reflex.web_streaming.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.web_streaming.processing.LegacyResourceStreamingServlet;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.security.web.api.AuthFilters;
import hiconic.rx.web.server.api.WebServerContract;
import jakarta.servlet.DispatcherType;

/** RX binding of the legacy resource URL protocol still used by existing web clients. */
@Managed
public class WebStreamingServerRxModuleSpace implements RxModuleContract {

	@Import private AccessContract access;
	@Import private WebServerContract webServer;

	@Override
	public void onDeploy() {
		access.setResourceStreamingUrlSupplier(() -> webServer.defaultEndpointUrl() + "/streaming");
		webServer.addServlet("legacy-resource-streaming", "streaming", streamingServlet());
		// ResourceUrlBuilder generates the exact /streaming URL with query parameters.
		// A servlet URL pattern ending in /* does not match that exact path, so both
		// mappings are needed to establish the authenticated request context.
		webServer.addFilterMapping(AuthFilters.strictAuthFilter, "/streaming", DispatcherType.REQUEST);
		webServer.addFilterMapping(AuthFilters.strictAuthFilter, "/streaming/*", DispatcherType.REQUEST);
	}

	@Managed
	private LegacyResourceStreamingServlet streamingServlet() {
		LegacyResourceStreamingServlet bean = new LegacyResourceStreamingServlet();
		bean.setSessionFactory(access.contextSessionFactory());
		return bean;
	}
}
