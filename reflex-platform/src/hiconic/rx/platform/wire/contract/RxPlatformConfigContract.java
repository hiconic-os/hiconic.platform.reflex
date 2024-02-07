package hiconic.rx.platform.wire.contract;

import java.io.File;

import com.braintribe.wire.api.space.WireSpace;

public interface RxPlatformConfigContract extends WireSpace {
	File appDir();
}
