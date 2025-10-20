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
package hiconic.rx.web.ddra.endpoints.api.api.v1;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.webapi.endpoints.OutputPrettiness;
import hiconic.rx.webapi.endpoints.TypeExplicitness;
import hiconic.rx.webapi.endpoints.api.v1.ApiV1DdraEndpoint;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import hiconic.rx.webapi.model.meta.RequestPath;
import hiconic.rx.webapi.model.meta.RequestPathPrefix;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This entity is meant to be used in the DdraMappings map, it is meant to be immutable to guarantee that DdraMappings is thread-safe.
 */
public interface SingleDdraMapping {

	String getServiceDomain();

	/** 
	 * {@link HttpServletRequest#getPathInfo() PathInfo} associated with this mapping, of the form: <code>/service-domain/possible-prefix/my-request</code>
	 *
	 * @see RequestPath
	 * @see RequestPathPrefix
	 */
	String getPathInfo();

	HttpRequestMethod getMethod();

	EntityType<? extends ServiceRequest> getRequestType();

	Boolean getDefaultSaveLocally();

	Boolean getDefaultDownloadResource();

	String getDefaultResponseFilename();

	String getDefaultResponseContentType();

	String getDefaultProjection();

	String getDefaultMimeType();

	Boolean getAnnounceAsMultipart();

	boolean getHideSerializedRequest();

	ServiceRequest getTransformRequest();

	// TODO why is ApiV1DdraEndpoint needed?
	ApiV1DdraEndpoint getEndpointPrototype();

	OutputPrettiness getDefaultPrettiness();

	String getDefaultDepth();

	Boolean getDefaultStabilizeOrder();

	Boolean getDefaultWriteEmptyProperties();

	Boolean getDefaultWriteAbsenceInformation();

	TypeExplicitness getDefaultTypeExplicitness();

	Integer getDefaultEntityRecurrenceDepth();

	Boolean getDefaultUseSessionEvaluation();

	Boolean getDefaultPreserveTransportPayload();

	Boolean getDefaultDecodingLenience();

	String getDefaultEndpointParameter(String name);
}
