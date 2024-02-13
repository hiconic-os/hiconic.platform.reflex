<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.spaceFull))}
package ${context.spacePackage};

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class ${context.spaceSimple} implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

}