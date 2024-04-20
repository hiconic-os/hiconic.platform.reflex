<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.spaceFull))}package ${context.spacePackage};

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
<#if request.sampleProcessor>

import ${context.greetProcessorFull};
import ${context.greetRequestFull};
</#if>

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class ${context.spaceSimple} implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

<#if request.sampleProcessor>
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(${context.greetRequestSimple}.T, this::greetProcessor);
	}

	@Managed
	private ${context.greetProcessorSimple} greetProcessor() {
		return new ${context.greetProcessorSimple}();
	}

</#if>
}