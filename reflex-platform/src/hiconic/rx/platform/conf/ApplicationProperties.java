package hiconic.rx.platform.conf;

import com.braintribe.wire.api.annotation.Default;

public interface ApplicationProperties {
	@Default("true")
	boolean consoleOutput();
	
	@Default("Reflex")
	String applicationName();
}
