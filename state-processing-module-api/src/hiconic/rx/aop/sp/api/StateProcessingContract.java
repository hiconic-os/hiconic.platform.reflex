package hiconic.rx.aop.sp.api;

import java.util.List;

import com.braintribe.model.processing.aop.api.aspect.AccessAspect;

import hiconic.rx.module.api.wire.RxExportContract;

/**
 * @author peter.gazdik
 */
public interface StateProcessingContract extends RxExportContract {

	/** Instantiates a state processing aspect with default rules, which for now is just the MetaDataStateChangeProcessorRule. */
	AccessAspect createStateProcessingAspect();

	AccessAspect createStateProcessingAspect(List<StateChangeProcessorRule> rules);

	StateProcessorRegistry stateProcessorRegistry();

	void registerStateChangeProcessor(StateChangeProcessor<?, ?> stateChangeProcessor);

}
