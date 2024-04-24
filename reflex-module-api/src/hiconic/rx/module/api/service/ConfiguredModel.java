package hiconic.rx.module.api.service;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.module.api.wire.RxPlatformContract;

public interface ConfiguredModel extends ModelReference {
	
	/**
	 * The {@link CmdResolver} contextualized with the system {@link AttributeContext} taken from {@link RxPlatformContract#systemAttributeContextSupplier()}
	 */
	CmdResolver systemCmdResolver();
	
	/**
	 * The {@link CmdResolver} contextualized with the thread associated {@link AttributeContext} taken from {@link AttributeContexts#peek()}
	 */
	CmdResolver contextCmdResolver();

	/**
	 * The {@link CmdResolver} contextualized with given {@link AttributeContext}
	 */
	CmdResolver cmdResolver(AttributeContext attributeContext);
	
	/**
	 * The {@link ModelOracle} resulting from all configurations done via the name associated {@link ModelConfiguration}.
	 */
	ModelOracle modelOracle();
}
