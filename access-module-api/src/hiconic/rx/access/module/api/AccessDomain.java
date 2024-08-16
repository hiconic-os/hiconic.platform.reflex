package hiconic.rx.access.module.api;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

public interface AccessDomain {
	PersistenceGmSession newContextSession();
	
	PersistenceGmSession newSystemSession();
	
	PersistenceGmSession newSession(AttributeContext attributeContext);
	
	CmdResolver contextCmdResolver();
	
	CmdResolver systemCmdResolver();
	
	CmdResolver cmdResolver(AttributeContext attributeContext);
}
