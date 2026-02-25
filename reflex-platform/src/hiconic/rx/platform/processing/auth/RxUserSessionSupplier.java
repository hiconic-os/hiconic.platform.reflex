package hiconic.rx.platform.processing.auth;

import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

public class RxUserSessionSupplier implements Supplier<UserSession> {
	private final Supplier<AttributeContext> attributeContextSupplier;
	
	public RxUserSessionSupplier() {
		this(AttributeContexts::peek);
	}
	
	public RxUserSessionSupplier(Supplier<AttributeContext> attributeContextSupplier) {
		this.attributeContextSupplier = attributeContextSupplier;
	}
	
	@Override
	public UserSession get() {
		return attributeContextSupplier.get().findOrNull(UserSessionAspect.class);
	}
	
	public String findSessionId() {
		UserSession userSession = get();
		return userSession != null? userSession.getSessionId(): null;
	}
}
