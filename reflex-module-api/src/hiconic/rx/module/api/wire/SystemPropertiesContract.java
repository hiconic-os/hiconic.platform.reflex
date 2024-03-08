package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.space.WireSpace;

/**
 * Marker interface indicating properties should be resolved from {@link System#getProperty(String) system properties}.
 * 
 * @see EnvironmentPropertiesContract
 */
public interface SystemPropertiesContract extends WireSpace {
	// empty
}
