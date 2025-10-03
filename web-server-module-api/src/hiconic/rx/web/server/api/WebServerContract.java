// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.web.server.api;

import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import hiconic.rx.module.api.wire.RxExportContract;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.Endpoint;

public interface WebServerContract extends RxExportContract {

	RemoteClientAddressResolver remoteAddressResolver();

	int getEffectiveServerPort();

	void addEndpoint(String path, Endpoint endpoint);

	// TODO document!!!
	void addServlet(String name, String path, HttpServlet servlet);
	void addServlet(String basePath, String name, String path, HttpServlet servlet);

	void addStaticFileResource(String path, String rootDir, String... welcomeFiles);

	/**
	 * URL with which the server can be reached from the outside. It is called public as it can be the URL of the proxy that propagates the request to
	 * the server behind it.
	 */
	String publicUrl();

	/**
	 * Resolves given path relative to the default endpoint path.<br>
	 * default endpoint path: services<br>
	 * path: x/y<br>
	 * result: services/x/y
	 */
	String resolveDefaultEndpointPath(String path);

	// TODO why is this here?
	String callerInfoFilterName();

	void addFilter(FilterSymbol name, Filter filter);
	void addFilter(String basePath, FilterSymbol name, Filter filter);

	void addFilterServletNameMapping(String filterName, String mapping, DispatcherType dispatcherType);
	void addFilterServletNameMapping(String basePath, String filterName, String mapping, DispatcherType dispatcherType);
	void addFilterMapping(String filterName, String mapping, DispatcherType dispatcherType);
	void addFilterMapping(String basePath, String filterName, String mapping, DispatcherType dispatcherType);

	// @formatter:off
	default void addFilterServletNameMapping(FilterSymbol name, String mapping, DispatcherType dispatcherType) { addFilterServletNameMapping(name.name(), mapping, dispatcherType); }
	default void addFilterServletNameMapping(String basePath, FilterSymbol name, String mapping, DispatcherType dispatcherType) { addFilterServletNameMapping(basePath, name.name(), mapping, dispatcherType); }
	default void addFilterMapping(FilterSymbol name, String mapping, DispatcherType dispatcherType) { addFilterMapping(name.name(), mapping, dispatcherType); }
	default void addFilterMapping(String basePath, FilterSymbol name, String mapping, DispatcherType dispatcherType) { addFilterMapping(basePath, name.name(), mapping, dispatcherType); }
	// @formatter:on

}
