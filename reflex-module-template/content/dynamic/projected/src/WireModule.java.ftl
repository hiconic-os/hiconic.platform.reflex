<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.wireModuleFull))}package ${context.wirePackage};

import hiconic.rx.module.api.wire.RxModule;

import hiconic.rx.module.api.wire.RxModuleContract;
import ${context.spaceFull};

public enum ${context.wireModuleSimple} implements RxModule<${context.spaceSimple}> {

	INSTANCE;

}