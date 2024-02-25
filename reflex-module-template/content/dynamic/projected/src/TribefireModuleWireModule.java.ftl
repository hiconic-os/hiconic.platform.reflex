<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.wireModuleFull))}package ${context.wirePackage};

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxModuleContract;
import ${context.spaceFull};

public enum ${context.wireModuleSimple} implements WireTerminalModule<RxModuleContract> {

	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, ${context.spaceSimple}.class);
	}

}