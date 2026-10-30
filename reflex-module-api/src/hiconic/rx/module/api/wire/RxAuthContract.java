package hiconic.rx.module.api.wire;

import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.auth.RoleAuthorization;

public interface RxAuthContract extends WireSpace {

	/**
	 * Prefix of the id of every {@link com.braintribe.model.usersession.UserSessionType#internal internal} session, such as the
	 * {@link #systemUserSession() system session}. A node authenticates only its own system session by such an id and rejects every
	 * other one.
	 */
	String INTERNAL_SESSION_ID_PREFIX = "i-";

	/**
	 * Direct access to the instance accessible via #systemUserSessionSupplier()
	 * <p>
	 * The system session exists only in the memory of this node and is never stored. Its id has the
	 * {@link #INTERNAL_SESSION_ID_PREFIX}. Only this node can authenticate it; other nodes reject it. Never send the id out of the
	 * process, e.g. in an HTTP call to the own services URL: behind a load balancer such a call fails whenever another node receives it.
	 * Evaluate the request in-process instead, or use messaging for work on other nodes.
	 */
	UserSession systemUserSession();
	User systemUser();

	Supplier<String> systemUserSessionIdSupplier();
	Supplier<String> contextUserSessionIdSupplier();
	Supplier<String> userSessionIdSupplier(AttributeContext attributeContext);
	
	Supplier<UserSession> systemUserSessionSupplier();
	Supplier<UserSession> contextUserSessionSupplier();
	Supplier<UserSession> userSessionSupplier(AttributeContext attributeContext);
	
	Supplier<AttributeContext> systemAttributeContextSupplier();
	Supplier<AttributeContext> contextAttributeContextSupplier();

	RoleAuthorization roleAuthorization();
}
