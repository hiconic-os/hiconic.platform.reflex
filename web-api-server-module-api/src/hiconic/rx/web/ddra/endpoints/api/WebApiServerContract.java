package hiconic.rx.web.ddra.endpoints.api;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.web.ddra.endpoints.api.v1.WebApiMappingOracle;

/**
 * @author peter.gazdik
 */
public interface WebApiServerContract extends RxExportContract {

	/** @see WebApiMappingOracle */
	WebApiMappingOracle mappingOracle();

	/**
	 * The path to the web-api servlet, without leading or trailing slash.
	 * <p>
	 * Default: <em>api</em>
	 */
	String servletPath();
}
