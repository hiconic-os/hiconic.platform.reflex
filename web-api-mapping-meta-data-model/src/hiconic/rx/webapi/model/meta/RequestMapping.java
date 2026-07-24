package hiconic.rx.webapi.model.meta;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/**
 * A complete, repeatable DDRA mapping. Unlike the individual request path and method metadata this type keeps all
 * mapping-specific values together and can therefore express several independent mappings for one request type.
 */
public interface RequestMapping extends EntityTypeMetaData {
	EntityType<RequestMapping> T = EntityTypes.T(RequestMapping.class);

	@Mandatory String getPath();
	void setPath(String path);
	@Mandatory HttpRequestMethod getMethod();
	void setMethod(HttpRequestMethod method);
	String getSection();
	void setSection(String section);
	String getResponseProjection();
	void setResponseProjection(String responseProjection);
	boolean getResponseAsResourcePayload();
	void setResponseAsResourcePayload(boolean responseAsResourcePayload);
	boolean getResponseWithDownloadDialog();
	void setResponseWithDownloadDialog(boolean responseWithDownloadDialog);
	String getResponseMimeType();
	void setResponseMimeType(String responseMimeType);
	boolean getHideSerializedRequest();
	void setHideSerializedRequest(boolean hideSerializedRequest);
	Boolean getAnnounceAsMultipart();
	void setAnnounceAsMultipart(Boolean announceAsMultipart);
	boolean getUseSessionEvaluation();
	void setUseSessionEvaluation(boolean useSessionEvaluation);
	Integer getEntityRecurrenceDepth();
	void setEntityRecurrenceDepth(Integer entityRecurrenceDepth);
	String getDepth();
	void setDepth(String depth);
	boolean getDecodingLenience();
	void setDecodingLenience(boolean decodingLenience);
}
