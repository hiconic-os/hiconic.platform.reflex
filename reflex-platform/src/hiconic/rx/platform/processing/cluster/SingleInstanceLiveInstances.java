package hiconic.rx.platform.processing.cluster;

import java.util.Collections;
import java.util.Set;

import com.braintribe.cfg.Required;
import com.braintribe.model.service.api.InstanceId;

import hiconic.rx.topology.api.LiveInstances;

public class SingleInstanceLiveInstances implements LiveInstances {
	private InstanceId instanceId;
	
	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}

	@Override
	public Set<String> liveInstances() {
		return Collections.singleton(instanceId.stringify());
	}

	@Override
	public Set<String> liveInstances(InstanceId matching) {
		// test if app is either wildcard or a match
		if (!(matching.getApplicationId() == null || matching.getApplicationId().equals(instanceId.getApplicationId())))
			return Collections.emptySet();
		
		// test if node is either wildcard or a match
		if (!(matching.getNodeId() == null || matching.getNodeId().equals(instanceId.getNodeId())))
			return Collections.emptySet();

		return liveInstances();
	}

	@Override
	public Set<String> liveApplications() {
		return Collections.singleton(instanceId.getApplicationId());
	}
}
