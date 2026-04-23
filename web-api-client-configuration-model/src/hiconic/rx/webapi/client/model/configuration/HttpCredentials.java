// ============================================================================
package hiconic.rx.webapi.client.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface HttpCredentials  extends GenericEntity {

	EntityType<HttpCredentials> T = EntityTypes.T(HttpCredentials.class);


}
