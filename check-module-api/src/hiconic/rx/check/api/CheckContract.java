package hiconic.rx.check.api;

import hiconic.rx.module.api.wire.RxExportContract;

/**
 * @author peter.gazdik
 */
public interface CheckContract extends RxExportContract {

	/**
	 * @see CheckProcessorRegistry
	 */
	CheckProcessorRegistry checkProcessorRegistry();

}
