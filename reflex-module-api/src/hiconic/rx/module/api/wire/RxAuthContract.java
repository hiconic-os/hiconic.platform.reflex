package hiconic.rx.module.api.wire;

import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.space.WireSpace;

public interface RxAuthContract extends WireSpace {
	Supplier<String> systemUserSessionIdSupplier();
	Supplier<String> contextUserSessionIdSupplier();
	Supplier<String> userSessionIdSupplier(AttributeContext attributeContext);
	
	Supplier<UserSession> systemUserSessionSupplier();
	Supplier<UserSession> contextUserSessionSupplier();
	Supplier<UserSession> userSessionSupplier(AttributeContext attributeContext);
	
	Supplier<AttributeContext> systemAttributeContextSupplier();
	Supplier<AttributeContext> contextAttributeContextSupplier();
}
