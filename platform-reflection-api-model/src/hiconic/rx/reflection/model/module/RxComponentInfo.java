package hiconic.rx.reflection.model.module;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * @author peter.gazdik
 */
@Abstract
public interface RxComponentInfo extends GenericEntity {

	EntityType<RxComponentInfo> T = EntityTypes.T(RxComponentInfo.class);

	String getGroupId();
	void setGroupId(String groupId);

	String getArtifactId();
	void setArtifactId(String artifactId);

	String getVersion();
	void setVersion(String version);

}
