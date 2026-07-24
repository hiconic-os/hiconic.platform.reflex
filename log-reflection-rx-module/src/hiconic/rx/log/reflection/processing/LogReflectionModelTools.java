package hiconic.rx.log.reflection.processing;

import com.braintribe.model.service.api.InstanceId;

import hiconic.rx.log.reflection.model.LogOrigin;

final class LogReflectionModelTools {
	private LogReflectionModelTools() {
	}

	static LogOrigin origin(InstanceId instanceId) {
		LogOrigin origin = LogOrigin.T.create();
		origin.setApplicationId(instanceId.getApplicationId());
		origin.setNodeId(instanceId.getNodeId());
		return origin;
	}
}
