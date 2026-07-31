package hiconic.rx.webapi.model.meta;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/**
 * A complete, repeatable DDRA mapping. Unlike the individual request path and method metadata this type keeps all
 * mapping-specific values together and can therefore express several independent mappings for one request type.
 * Complete mappings coexist with explicitly declared fine-grained {@link RequestPath}/{@link RequestMethod} bindings;
 * a {@link RequestPathPrefix} applies to both forms.
 */
public interface RequestMapping extends EntityTypeMetaData {
	EntityType<RequestMapping> T = EntityTypes.T(RequestMapping.class);

	/**
	 * A complete mapping declares a concrete endpoint and must therefore not silently propagate to request subtypes.
	 * Explicit inheritance remains possible through {@link #setInherited(boolean)}.
	 */
	@Override
	@Initializer("false")
	boolean getInherited();

	@Mandatory String getPath();
	void setPath(String path);
	@Mandatory HttpRequestMethod getMethod();
	void setMethod(HttpRequestMethod method);

	/** Empty means inherit {@link RequestSection}. */
	String getSection();
	void setSection(String section);

	String getResponseProjection();
	void setResponseProjection(String responseProjection);

	/** {@code null} or {@link BooleanOverride#INHERIT} means inherit {@link ResponseAsResourcePayload}. */
	BooleanOverride getResponseAsResourcePayload();
	void setResponseAsResourcePayload(BooleanOverride responseAsResourcePayload);

	/** {@code null} or {@link BooleanOverride#INHERIT} means inherit {@link ResponseWithDownloadDialog}. */
	BooleanOverride getResponseWithDownloadDialog();
	void setResponseWithDownloadDialog(BooleanOverride responseWithDownloadDialog);

	/** Empty means inherit {@link ResponseMimeType}. */
	String getResponseMimeType();
	void setResponseMimeType(String responseMimeType);

	/** {@code null} or {@link BooleanOverride#INHERIT} means inherit {@link HideSerializedRequest}. */
	BooleanOverride getHideSerializedRequest();
	void setHideSerializedRequest(BooleanOverride hideSerializedRequest);

	/** {@code null} or {@link BooleanOverride#INHERIT} means inherit {@link ResponseAsMultipart}. */
	BooleanOverride getAnnounceAsMultipart();
	void setAnnounceAsMultipart(BooleanOverride announceAsMultipart);

	/** {@code null} or {@link BooleanOverride#INHERIT} means inherit {@link RequestEvaluateWithSession}. */
	BooleanOverride getUseSessionEvaluation();
	void setUseSessionEvaluation(BooleanOverride useSessionEvaluation);

	/** {@code null} means inherit {@link ResponseEntityRecurrenceDepth}. */
	Integer getEntityRecurrenceDepth();
	void setEntityRecurrenceDepth(Integer entityRecurrenceDepth);

	/** Empty means inherit {@link ResponseDepth}. */
	String getDepth();
	void setDepth(String depth);

	/** {@code null} or {@link BooleanOverride#INHERIT} means inherit {@link RequestDecodingLenience}. */
	BooleanOverride getDecodingLenience();
	void setDecodingLenience(BooleanOverride decodingLenience);
}
