package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.space.WireSpace;
import com.braintribe.wire.impl.properties.PropertyLookups;

/**
 * Marker interface for contracts dedicated to resolving properties, defined as:
 * <ul>
 * <li>System properties
 * <li>environment variables
 * <li>properties from <code>properties.yaml</code> config file set (i.e. including any name with pattern properties-suffix.yaml)
 * </ul>
 * <p>
 * These contracts are implemented automatically.
 * <p>
 * Each method is assumed to be the name of a property and the implementation returns the corresponding value.
 * <p>
 * Example:
 * 
 * <pre>
 * public interface XyzPropertiesContract extends RxPropertiesContract {
 * 	String EXTERNAL_SERVICE_URL();
 * 
 * 	&#64;Default("4")
 * 	Integer NUMBER_OF_THREADS_FOR_SOME_TASK();
 * 
 * 	&#64;Decrypt
 * 	String SOME_DB_PASSWORD();
 * }
 * </pre>
 * 
 * @see PropertyLookups
 */
public interface RxPropertiesContract extends WireSpace {
	// empty
}
