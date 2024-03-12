package hiconic.rx.platform.conf;

import java.io.File;

import com.braintribe.wire.api.annotation.Default;
import com.braintribe.wire.api.annotation.Name;

public interface SystemProperties {
	static String PROPERTY_APP_DIR = "reflex.app.dir";
	static String PROPERTY_LAUNCH_SCRIPT = "reflex.launch.script"; 
	
	@Name(PROPERTY_APP_DIR)
	@Default(".")
	File appDir();
	
	@Name(PROPERTY_LAUNCH_SCRIPT)
	@Default("launch-script")
	String launchScript();
}
