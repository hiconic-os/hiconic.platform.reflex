package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.space.WireSpace;

/**
 * Marker interface indicating properties should be resolved from {@link System#getenv(String) the environment}.
 * 
 * @see SystemPropertiesContract
 */
public interface EnvironmentPropertiesContract extends WireSpace {
	// empty
}
