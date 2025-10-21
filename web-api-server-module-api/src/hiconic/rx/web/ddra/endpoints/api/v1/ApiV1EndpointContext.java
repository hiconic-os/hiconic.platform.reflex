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

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.session.InputStreamProvider;
import com.braintribe.model.generic.session.OutputStreamProvider;
import com.braintribe.model.processing.rpc.commons.impl.RpcMarshallingStreamManagement;
import com.braintribe.model.processing.rpc.commons.impl.RpcUnmarshallingStreamManagement;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.StringTools;
import com.braintribe.web.multipart.api.MultipartFormat;
import com.braintribe.web.multipart.impl.Multiparts;

import hiconic.rx.web.ddra.endpoints.api.DdraEndpointContext;
import hiconic.rx.webapi.endpoints.api.v1.ApiV1DdraEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiV1EndpointContext extends DdraEndpointContext<ApiV1DdraEndpoint> {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ApiV1EndpointContext.class);

	private static final String MULTIPART_FORM_DATA = "multipart/form-data";
	private final String defaultServiceDomain;

	private SingleDdraMapping mapping;
	// private ServiceRequest serviceRequest;
	private EntityType<? extends ServiceRequest> explicitServiceRequestType;

	private String explicitServiceDomain;

	private final RpcMarshallingStreamManagement responseStreamManagement;
	private RpcUnmarshallingStreamManagement requestStreamManagement;
	private OutputStreamProvider responseOutputStreamProvider;

	private GenericModelType expectedResponseType;
	private InputStreamProvider requestTransportPayload;

	public ApiV1EndpointContext(HttpServletRequest request, HttpServletResponse response, String defaultServiceDomain) {
		super(request, response);
		responseStreamManagement = new RpcMarshallingStreamManagement();
		this.defaultServiceDomain = defaultServiceDomain;
	}

	public RpcMarshallingStreamManagement getResponseStreamManagement() {
		return responseStreamManagement;
	}

	public void setRequestStreamManagement(RpcUnmarshallingStreamManagement requestStreamManagement) {
		this.requestStreamManagement = requestStreamManagement;
	}

	public RpcUnmarshallingStreamManagement getRequestStreamManagement() {
		return requestStreamManagement;
	}

	public MultipartFormat getRequestMultipartFormat() {
		String mimeType = getRequest().getContentType();

		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}

		return Multiparts.parseFormat(mimeType);

	}

	public boolean isMultipartResponse() {
		return !StringTools.isBlank(getEndpoint().getResponseContentType()) ? getEndpoint().getResponseContentType().contains(MULTIPART_FORM_DATA)
				: getEndpoint().getAccept().contains(MULTIPART_FORM_DATA);
	}

	public boolean isResourceDownloadResponse() {
		return getEndpoint().getDownloadResource();
	}

	public void setMultipartResponseContentType(String boundary) {
		getResponse().setContentType(MULTIPART_FORM_DATA + ";boundary=" + boundary);
		getResponse().setStatus(200);
	}

	public void ensureContentDispositionHeader(String defaultFileName) {
		String responseFilename = getEndpoint().getResponseFilename();
		if (responseFilename == null) {
			responseFilename = defaultFileName;
		}

		StreamHeaderUtils.ensureContentDispositionHeader(responseFilename, getEndpoint().getSaveLocally(), getResponse());

	}

	@Override
	public Consumer<GenericEntity> getMarshallingVisitor() {
		return responseStreamManagement.getMarshallingVisitor();
	}

	@Override
	public OutputStream openResponseOutputStream() throws IOException {
		if (responseOutputStreamProvider == null)
			return super.openResponseOutputStream();

		return responseOutputStreamProvider.openOutputStream();
	}

	public void setResponseOutputStreamProvider(OutputStreamProvider responseOutputStreamProvider) {
		this.responseOutputStreamProvider = responseOutputStreamProvider;
	}

	public SingleDdraMapping getMapping() {
		return mapping;
	}

	public void setMapping(SingleDdraMapping mapping) {
		this.mapping = mapping;
	}

	public String getDefaultMimeType() {
		return this.mapping == null ? null : this.mapping.getDefaultMimeType();
	}

	public String getServiceDomain() {
		if (explicitServiceDomain != null)
			return explicitServiceDomain;

		if (mapping != null && mapping.getServiceDomain() != null) {
			return mapping.getServiceDomain();
		}

		return defaultServiceDomain;
	}

	public void setServiceDomain(String serviceDomain) {
		this.explicitServiceDomain = serviceDomain;
	}

	public void setServiceRequestType(EntityType<? extends ServiceRequest> serviceRequestType) {
		this.explicitServiceRequestType = serviceRequestType;
	}

	public EntityType<? extends ServiceRequest> getServiceRequestType() {
		if (mapping != null)
			return mapping.getRequestType();

		return explicitServiceRequestType;
	}

	public void setExpectedResponseType(GenericModelType expectedResponseType) {
		this.expectedResponseType = expectedResponseType;
	}

	public GenericModelType getExpectedResponseType() {
		return expectedResponseType;
	}

	public boolean useSessionEvaluation() {
		Boolean useSessionEvaluation = getEndpoint().getUseSessionEvaluation();
		return useSessionEvaluation == null ? true : useSessionEvaluation;
	}

	public InputStreamProvider getRequestTransportPayload() {
		return requestTransportPayload;
	}

	public void setRequestTransportPayload(InputStreamProvider requestTransportPayload) {
		this.requestTransportPayload = requestTransportPayload;
	}

	// public ServiceRequest getServiceRequest() {
	// return serviceRequest;
	// }
	//
	// public void setServiceRequest(ServiceRequest serviceRequest) {
	// this.serviceRequest = serviceRequest;
	// }

}
