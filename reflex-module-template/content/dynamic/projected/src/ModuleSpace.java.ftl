<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.spaceFull))}package ${context.spacePackage};

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
<#if request.sampleProcessor>

import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;

import ${context.modelReflectionFull};
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
	public void addApiModels(ConfigurationModelBuilder builder) {
		builder.addDependency(${context.modelReflectionSimple}.reflection);
	}

	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.register(${context.greetRequestSimple}.T, greetProcessor());
	}

	@Managed
	private ${context.greetProcessorSimple} greetProcessor() {
		return new ${context.greetProcessorSimple}();
	}

</#if>
}