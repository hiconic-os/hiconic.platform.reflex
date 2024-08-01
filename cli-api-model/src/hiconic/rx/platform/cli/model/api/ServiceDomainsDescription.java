package hiconic.rx.platform.cli.model.api;

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.GmMetaModel;

public interface ServiceDomainsDescription extends GenericEntity {

	EntityType<ServiceDomainsDescription> T = EntityTypes.T(ServiceDomainsDescription.class);

	Map<String, GmMetaModel> getDomainNameToModel();
	void setDomainNameToModel(Map<String, GmMetaModel> domainNameToModel);

}
