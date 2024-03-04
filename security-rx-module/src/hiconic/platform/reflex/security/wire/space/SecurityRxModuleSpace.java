package hiconic.platform.reflex.security.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class SecurityRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private DatabaseContract database;

}