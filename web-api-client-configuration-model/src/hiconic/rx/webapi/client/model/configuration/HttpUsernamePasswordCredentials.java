// ============================================================================
package hiconic.rx.webapi.client.model.configuration;

import com.braintribe.model.descriptive.HasCredentials;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface HttpUsernamePasswordCredentials extends HttpCredentials, HasCredentials {

	EntityType<HttpUsernamePasswordCredentials> T = EntityTypes.T(HttpUsernamePasswordCredentials.class);

}
