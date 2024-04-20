package hiconic.rx.access.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface Access extends GenericEntity {
	EntityType<Access> T = EntityTypes.T(Access.class);

	String getName();
	void setName(String name);
	
	String getDataModelName();
	String getConfigurationModelName();
	
	String getServiceModelName();
}
