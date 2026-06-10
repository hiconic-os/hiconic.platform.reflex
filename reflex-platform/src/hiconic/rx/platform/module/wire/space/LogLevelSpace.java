package hiconic.rx.platform.module.wire.space;

import java.util.Set;

import com.braintribe.gm.logging.level.LogLevelApplicationResolver;
import com.braintribe.gm.logging.level.LogLevelServiceProcessor;
import com.braintribe.gm.logging.level.MulticastingLogLevelUpdateDispatcher;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.logging.level.LogLevelManager;
import com.braintribe.logging.level.LogLevelRuntimeUpdater;
import com.braintribe.logging.level.LogLevelSetup;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;

@Managed
public class LogLevelSpace implements WireSpace {
	@Import
	private ExtendedRxPlatformContract platform;

	@Managed
	public LogLevelServiceProcessor logLevelProcessor() {
		LogLevelServiceProcessor bean = new LogLevelServiceProcessor();
		bean.setLogLevelManager(logLevelManager());
		bean.setLogLevelRuntimeUpdater(logLevelRuntimeUpdater());
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setLocalInstanceId(platform.application().instanceId());
		bean.setApplicationResolver(logLevelApplicationResolver());
		return bean;
	}

	@Managed
	public LogLevelManager logLevelManager() {
		return LogLevelSetup.instance().logLevelManager();
	}

	@Managed
	private LogLevelRuntimeUpdater logLevelRuntimeUpdater() {
		LogLevelRuntimeUpdater bean = new LogLevelRuntimeUpdater();
		bean.setLogLevelManager(logLevelManager());
		bean.setUpdateDispatcher(logLevelUpdateDispatcher());
		return bean;
	}

	@Managed
	private MulticastingLogLevelUpdateDispatcher logLevelUpdateDispatcher() {
		MulticastingLogLevelUpdateDispatcher bean = new MulticastingLogLevelUpdateDispatcher();
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setLocalInstanceId(platform.application().instanceId());
		bean.setLogLevelManager(logLevelManager());
		return bean;
	}

	@Managed
	private LogLevelApplicationResolver logLevelApplicationResolver() {
		return new LogLevelApplicationResolver() {
			@Override
			public Set<String> liveApplications() {
				return platform.application().liveInstances().liveApplications();
			}

			@Override
			public Maybe<InstanceId> resolveApplication(String applicationId) {
				InstanceId localInstanceId = platform.application().instanceId();
				if (applicationId == null || applicationId.equals(localInstanceId.getApplicationId())) {
					return Maybe.complete(localInstanceId);
				}

				Set<String> instances = platform.application().liveInstances().liveInstances(InstanceId.of(null, applicationId));
				if (instances == null || instances.isEmpty()) {
					return Maybe.empty(InvalidArgument.create("No live instance found for application: " + applicationId));
				}

				return Maybe.complete(InstanceId.parse(instances.iterator().next()));
			}
		};
	}
}
