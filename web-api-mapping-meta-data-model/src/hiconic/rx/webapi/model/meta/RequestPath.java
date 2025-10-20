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
package hiconic.rx.webapi.model.meta;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/**
 * This and the {@link RequestPathPrefix} together define how a request path (URL) is mapped to a concrete service request.
 * <p>
 * The path is relative to the path of the service domain.
 * 
 * <p>
 * <b>Example:</b> Let's say we have:
 * <ul>
 * <li><b>Service domain:</b> <code>my-domain</code>
 * <li><b>Request path prefix:</b> <code>my-prefix</code>
 * <li><b>Request path:</b> <code>my-request</code>
 * </ul>
 * 
 * Then the mapped path that is compared to the requests pathInfo is: <code>/my-domain/my-prefix/my-request</code>
 * <p>
 * Annotation: {@link hiconic.rx.webapi.model.annotation.RequestPath}
 * 
 * @author peter.gazdik
 */
public interface RequestPath extends EntityTypeMetaData {

	EntityType<RequestPath> T = EntityTypes.T(RequestPath.class);

	/**
	 * The actual path that together with the {@link RequestPathPrefix} defines the URL path mapped to the configured request.
	 * <p>
	 * The value may (but must not) start/end with '/'.
	 */
	@Mandatory
	String getPath();
	void setPath(String path);

	// TODO support property mappings later

	// List<String> getMandatoryProperties();
	// void setMandatoryProperties(List<String> mandatoryProperties);
	//
	// List<String> getOptionalProperties();
	// void setOptionalProperties(List<String> optionalProperties);

}
