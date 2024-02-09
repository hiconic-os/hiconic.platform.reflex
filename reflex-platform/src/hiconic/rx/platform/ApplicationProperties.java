package hiconic.rx.platform;

import com.braintribe.wire.api.annotation.Default;
import com.braintribe.wire.api.annotation.Name;

public interface ApplicationProperties {
	@Name("console.output")
	@Default("true")
	boolean consoleOutput();
}
