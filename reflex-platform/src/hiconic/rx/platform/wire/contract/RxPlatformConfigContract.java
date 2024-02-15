package hiconic.rx.platform.wire.contract;

import java.io.File;

import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.platform.conf.ApplicationProperties;

public interface RxPlatformConfigContract extends WireSpace {
	ApplicationProperties properties();
	File appDir();
	String launchScriptName();
	String[] cliArguments();
}
