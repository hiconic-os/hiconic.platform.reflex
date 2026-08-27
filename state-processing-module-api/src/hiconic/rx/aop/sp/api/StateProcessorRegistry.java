package hiconic.rx.aop.sp.api;

/**
 * @author peter.gazdik
 */
public interface StateProcessorRegistry {

	StateChangeProcessor<?, ?> getStateChangeProcessor(String processorId);
	
}
