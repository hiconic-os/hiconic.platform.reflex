package hiconic.rx.platform.conf;

import java.io.File;

import com.braintribe.wire.api.annotation.Default;
import com.braintribe.wire.api.annotation.Name;

public interface SystemProperties {
	@Name("reflex.app.dir")
	@Default(".")
	File appDir();
	
	@Name("reflex.launch.script")
	@Default("launch-script")
	String launchScript();
}
