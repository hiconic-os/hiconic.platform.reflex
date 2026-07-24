package hiconic.rx.webapi.client.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Modeled configuration of the Web API remote processors exposed by an application. */
public interface WebApiRemoteProcessorConfiguration extends GenericEntity {

	EntityType<WebApiRemoteProcessorConfiguration> T = EntityTypes.T(WebApiRemoteProcessorConfiguration.class);

	List<WebApiRemoteProcessor> getProcessors();
	void setProcessors(List<WebApiRemoteProcessor> processors);
}
