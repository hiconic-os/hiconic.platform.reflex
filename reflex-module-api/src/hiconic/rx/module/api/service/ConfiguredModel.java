package hiconic.rx.module.api.service;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.collection.impl.AttributeContexts;

public interface ConfiguredModel extends ModelReference {
	
	CmdResolver cmdResolver();
	
	CmdResolver systemCmdResolver();
	
	default CmdResolver contextCmdResolver() { return cmdResolver(AttributeContexts.peek()); }

	CmdResolver cmdResolver(AttributeContext attributeContext);
	
	ModelOracle modelOracle();
}
