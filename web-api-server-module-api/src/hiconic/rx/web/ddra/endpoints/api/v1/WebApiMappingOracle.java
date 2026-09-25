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
package hiconic.rx.web.ddra.endpoints.api.v1;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import hiconic.rx.webapi.model.meta.RequestPath;
import hiconic.rx.webapi.model.meta.RequestPathPrefix;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Provides information related to WebApi mappings for {@link ServiceRequest}s of all {@link ServiceDomain}s.
 * 
 * The implementation must be thread-safe!!!
 * 
 * @see RequestPathPrefix
 * @see RequestPath
 */
public interface WebApiMappingOracle {

	/** Resolves a mapping for given {@link HttpServletRequest#getPathInfo() pathInfo} and HTTP method. */
	SingleDdraMapping get(String pathInfo, HttpRequestMethod method);

	/**
	 * Resolves all mapped HTTP methods for given {@link HttpServletRequest#getPathInfo() pathInfo}, sorted alphabetically.
	 * <p>
	 * The returned values are names of {@link HttpRequestMethod} constants.
	 */
	List<String> getMethods(String pathInfo);

	/** Resolves all mappings within given {@link ServiceDomain service domain} */
	List<SingleDdraMapping> getAllForDomain(String serviceDomain);

	/**
	 * Resolves the implicit mapping of given request type, i.e. the mapping of the generic <code>/${domain}/${requestType}</code> path, which is
	 * declared by no meta data and exists for every request type.
	 * <p>
	 * There might still be DDRA mappings configured on the request, e.g. the {@link SingleDdraMapping#getDefaultMimeType() mime type} that selects
	 * the response marshaller. MD that belongs to an explicit mapping of the same type is not considered, as it configures that mapping only.
	 * <p>
	 * Returns <tt>null</tt> if there is no such service domain.
	 */
	SingleDdraMapping getImplicitMapping(String serviceDomain, EntityType<? extends ServiceRequest> requestType, HttpRequestMethod method);

}
