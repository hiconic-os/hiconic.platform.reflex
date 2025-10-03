package hiconic.rx.platform.conf;

import com.braintribe.codec.marshaller.api.ConfigurableMarshallerRegistry;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.provider.Box;

import hiconic.rx.module.api.wire.RxPlatformConfigurator;

/**
 * @author peter.gazdik
 */
public class RxPlatformConfiguratorImpl implements RxPlatformConfigurator {

	public Box<WorkerManager> workerManagerHolder;
	public ConfigurableMarshallerRegistry marshallerRegistry;
	public ConfigurableWorkerAspectRegistry workerAspectRegistry;

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

}
