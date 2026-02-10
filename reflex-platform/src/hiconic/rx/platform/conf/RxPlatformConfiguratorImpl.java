package hiconic.rx.platform.conf;

import com.braintribe.codec.marshaller.api.ConfigurableMarshallerRegistry;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.provider.Box;

import hiconic.rx.module.api.resource.ResourceStorageDeploymentExpert;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.platform.resource.RxResourcesStorages;
import hiconic.rx.resource.model.configuration.ResourceStorage;

/**
 * @author peter.gazdik
 */
public class RxPlatformConfiguratorImpl implements RxPlatformConfigurator {

	public Box<WorkerManager> workerManagerHolder;
	public ConfigurableMarshallerRegistry marshallerRegistry;
	public ConfigurableWorkerAspectRegistry workerAspectRegistry;
	public RxResourcesStorages resourceStorages;

	@Override
	public void setWorkerManager(WorkerManager workerManager) {
		workerManagerHolder.value = workerManager;
	}

	@Override
	public ConfigurableMarshallerRegistry marshallerRegistry() {
		return marshallerRegistry;
	}

	@Override
	public ConfigurableWorkerAspectRegistry workerAspectRegistry() {
		return workerAspectRegistry;
	}

	@Override
	public <RS extends ResourceStorage> void registerResourceStorageDeploymentExpert(EntityType<RS> storageType,
			EntityType<? extends ResourceSource> sourceType, ResourceStorageDeploymentExpert<RS> expert) {
		resourceStorages.registerDeploymentExpert(storageType, sourceType, expert);
	}

}
