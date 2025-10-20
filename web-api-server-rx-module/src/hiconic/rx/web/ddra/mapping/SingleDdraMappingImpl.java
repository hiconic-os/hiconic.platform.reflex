// ============================================================================
package hiconic.rx.web.ddra.mapping;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.StandardCloningContext;
//import com.braintribe.model.prototyping.api.PrototypingRequest;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.web.ddra.endpoints.api.api.v1.SingleDdraMapping;
import hiconic.rx.webapi.endpoints.OutputPrettiness;
import hiconic.rx.webapi.endpoints.TypeExplicitness;
import hiconic.rx.webapi.endpoints.api.v1.ApiV1DdraEndpoint;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;

/**
 */
public class SingleDdraMappingImpl implements SingleDdraMapping {

	public String serviceDomain;
	public String pathInfo;
	public HttpRequestMethod method;
	public EntityType<? extends ServiceRequest> requestType;

	public ServiceRequest transformRequest;
	public ApiV1DdraEndpoint endpointPrototype;
	public Boolean announceAsMultipart;
	public boolean hideSerializedRequest;

	public String defaultProjection;
	public String defaultMimeType;

	public OutputPrettiness defaultPrettiness;
	public String defaultDepth;
	public Boolean defaultStabilizeOrder;
	public Boolean defaultWriteEmptyProperties;
	public Boolean defaultWriteAbsenceInformation;
	public TypeExplicitness defaultTypeExplicitness;
	public Integer defaultEntityRecurrenceDepth;

	public Boolean defaultDownloadResource;
	public Boolean defaultSaveLocally;
	public String defaultResponseFilename;
	public String defaultResponseContentType;
	public Boolean defaultUseSessionEvaluation;
	public Boolean defaultPreserveTransportPayload;
	public Boolean defaultDecodingLenience;

	// @formatter:off
	@Override public String getServiceDomain() { return serviceDomain; }
	@Override public String getPathInfo() { return pathInfo; }
	@Override public HttpRequestMethod getMethod() { return method; }
	@Override public EntityType<? extends ServiceRequest> getRequestType() { return requestType; }

	@Override public Boolean getDefaultSaveLocally() { return defaultSaveLocally; }
	@Override public Boolean getDefaultDownloadResource() { return defaultDownloadResource; }
	@Override public String getDefaultResponseFilename() { return defaultResponseFilename; }
	@Override public String getDefaultResponseContentType() { return defaultResponseContentType; }
	@Override public String getDefaultProjection() { return defaultProjection; }
	@Override public String getDefaultMimeType() { return defaultMimeType; }
	@Override public Boolean getAnnounceAsMultipart() { return announceAsMultipart; }
	@Override public boolean getHideSerializedRequest() { return hideSerializedRequest; }
	@Override public ApiV1DdraEndpoint getEndpointPrototype() { return endpointPrototype == null ? null : endpointPrototype.clone(new StandardCloningContext()); }
	@Override public ServiceRequest getTransformRequest() { return transformRequest; }
	@Override public OutputPrettiness getDefaultPrettiness() { return defaultPrettiness; }
	@Override public String getDefaultDepth() { return defaultDepth; }
	@Override public Boolean getDefaultStabilizeOrder() { return defaultStabilizeOrder; }
	@Override public Boolean getDefaultWriteEmptyProperties() { return defaultWriteEmptyProperties; }
	@Override public Boolean getDefaultWriteAbsenceInformation() { return defaultWriteAbsenceInformation; }
	@Override public TypeExplicitness getDefaultTypeExplicitness() { return defaultTypeExplicitness; }
	@Override public Integer getDefaultEntityRecurrenceDepth() { return defaultEntityRecurrenceDepth; }
	@Override public Boolean getDefaultUseSessionEvaluation() { return defaultUseSessionEvaluation; }
	@Override public Boolean getDefaultPreserveTransportPayload() { return defaultPreserveTransportPayload; }
	@Override public Boolean getDefaultDecodingLenience() { return defaultDecodingLenience; }
	// @formatter:on

	@Override
	public String getDefaultEndpointParameter(String name) {
		switch (name) {
			case "depth":
				return defaultDepth;
			case "projection":
				return defaultProjection;
			case "prettiness":
				return defaultPrettiness != null ? defaultPrettiness.name() : null;
			case "entityRecurrenceDepth":
				return defaultEntityRecurrenceDepth != null ? String.valueOf(defaultEntityRecurrenceDepth) : null;
			case "mimeType":
				return defaultMimeType;
			case "serviceDomain":
				return serviceDomain;
			case "typeExplicitness":
				return defaultTypeExplicitness != null ? defaultTypeExplicitness.name() : null;
			case "stabilizeOrder":
				return defaultStabilizeOrder != null ? String.valueOf(defaultStabilizeOrder) : null;
			case "writeAbsenceInformation":
				return this.defaultWriteAbsenceInformation != null ? String.valueOf(defaultWriteAbsenceInformation) : null;
			case "writeEmptyProperties":
				return this.defaultWriteEmptyProperties != null ? String.valueOf(defaultWriteEmptyProperties) : null;
			case "responseContentType":
				return defaultResponseContentType;
			case "responseFilename":
				return defaultResponseFilename;
			case "saveLocally":
				return defaultSaveLocally != null ? String.valueOf(defaultSaveLocally) : null;
			case "downloadResource":
				return defaultDownloadResource != null ? String.valueOf(defaultDownloadResource) : null;
			case "defaultUseSessionEvaluation":
				return defaultUseSessionEvaluation != null ? String.valueOf(defaultUseSessionEvaluation) : null;
			default:
				return null;
		}
	}

}
