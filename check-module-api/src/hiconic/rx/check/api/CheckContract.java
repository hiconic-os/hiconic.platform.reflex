package hiconic.rx.check.api;

import com.braintribe.codec.marshaller.api.Marshaller;

import hiconic.rx.module.api.wire.RxExportContract;

/**
 * @author peter.gazdik
 */
public interface CheckContract extends RxExportContract {

	/**
	 * @see CheckProcessorRegistry
	 */
	CheckProcessorRegistry checkProcessorRegistry();

	Marshaller checkResultToHtmlMarshaller();
	
}
