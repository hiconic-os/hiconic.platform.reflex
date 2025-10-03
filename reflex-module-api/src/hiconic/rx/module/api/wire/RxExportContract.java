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
package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.space.WireSpace;

/**
 * Marker interface for {@link WireSpace contracts} that can be exported by one module and imported by another. This is the main mechanism how a
 * module offers an extension point for other modules to use.
 * <p>
 * Note that the importing module (typically) doesn't specify the module which exports the contract. It's up to the actual application to choose the
 * module it likes.
 * 
 * <h2>Module Dependencies</h2>
 * 
 * The export mechanism loosely defines inter-module dependencies:<br>
 * <b>All modules that import a given contract depend on the one module that exports it.</b>
 * <p>
 * This dependency influences the order in which modules are loaded. While independent modules can be loaded concurrently, a dependency is always
 * loaded first, before all it's dependers.
 * <p>
 * This implies no dependency cycle is possible and the application immediately fails if such a cycle is detected.
 * 
 * <h2>Example</h2>
 * 
 * <b>The contract:</b>
 * 
 * <pre>
 * public interface ServletsContract extends RxExportContract {
 * 	ServletRegistry servletRegistry();
 * }
 * </pre>
 * 
 * <b>A web-server module exports this contract via {@link RxModule#bindExports(Exports)}:</b>
 * 
 * <pre>
 * public enum MyWebServerRxModule implements RxModule&lt;MyWebServerModuleContract&gt; {
 * 	INSTANCE;
 * 
 * 	&#64;Override
 * 	public void bindExports(Exports exports) {
 * 		exports.bind(ServletsContract.class, MyWebServerServletsSpace.class);
 * 	}
 * }
 * </pre>
 * 
 * <b>Other modules simply import it:</b>
 * 
 * <pre>
 * public class MyServletSpace implements WireSpace, InitializationAware {
 * 	&#64;Import
 * 	private ServletsContract servlets;
 * 
 * 	&#64;Override
 * 	public void postConstruct() {
 * 		servlets.servletRegistry().registerServlet(myServlet);
 * 	}
 *   
 * 	private Servlet myServlet {
 * 		MyServlet bean = new MyServlet();
 * 		...
 * 		return bean;
 * 	}
 * </pre>
 * 
 * @see RxModule#bindExports(Exports)
 * @see Exports
 */
public interface RxExportContract extends WireSpace {
	// empty
}
