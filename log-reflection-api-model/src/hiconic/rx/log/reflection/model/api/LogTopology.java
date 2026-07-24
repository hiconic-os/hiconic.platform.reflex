package hiconic.rx.log.reflection.model.api;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.log.reflection.model.LogOrigin;

/** Stable, sorted snapshot of live application/node pairs. */
public interface LogTopology extends GenericEntity {
	EntityType<LogTopology> T = EntityTypes.T(LogTopology.class);

	List<LogOrigin> getInstances();
	void setInstances(List<LogOrigin> instances);

	LogOrigin getLocalInstance();
	void setLocalInstance(LogOrigin localInstance);
}
