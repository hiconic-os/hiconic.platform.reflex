package hiconic.rx.access.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface Access extends GenericEntity {
	EntityType<Access> T = EntityTypes.T(Access.class);

	@Mandatory
	String getAccessId();
	void setAccessId(String accessId);

	@Mandatory
	String getDataModelName();
	void setDataModelName(String dataModelName);
	
	/**
	 * Optional model for PersistenceRequest binding
	 */
	String getServiceModelName();
	void setServiceModelName(String serviceModelName);
	
	/**
	 * Optional id for a ServiceDomain for PersistenceRequest binding
	 */
	String getServiceDomainId();
	void setServiceDomainId(String serviceDomainId);
}
