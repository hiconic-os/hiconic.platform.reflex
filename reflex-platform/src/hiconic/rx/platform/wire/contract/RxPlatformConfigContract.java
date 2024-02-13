package hiconic.rx.platform.wire.contract;

import java.io.File;

import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.platform.ApplicationProperties;

public interface RxPlatformConfigContract extends WireSpace {
	ApplicationProperties properties();
	File appDir();
	String[] cliArguments();
}
