package hiconic.rx.access.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface AccessConfiguration extends GenericEntity {
	EntityType<AccessConfiguration> T = EntityTypes.T(AccessConfiguration.class);

	List<Access> getAccesses();
	void setAccesses(List<Access> accesses);
}
