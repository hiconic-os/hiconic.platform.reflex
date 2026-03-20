package hiconic.rx.platform.wire.space;

import static com.braintribe.wire.api.util.Sets.set;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.platform.processing.auth.RxUserSessionSupplier;

@Managed
public class RxAuthSpace implements RxAuthContract {
	
	@Override
	public Supplier<String> contextUserSessionIdSupplier() {
		return contextUserSessionSupplier()::findSessionId;
	}

	@Override
	public Supplier<String> systemUserSessionIdSupplier() {
		return systemUserSessionSupplier()::findSessionId;
	}
	
	@Override
	public Supplier<String> userSessionIdSupplier(AttributeContext attributeContext) {
		return userSessionSupplier(attributeContext)::findSessionId;
	}

	@Override
	@Managed
	public RxUserSessionSupplier contextUserSessionSupplier() {
		return new RxUserSessionSupplier();
	}

	@Override
	@Managed
	public RxUserSessionSupplier systemUserSessionSupplier() {
		return new RxUserSessionSupplier(systemAttributeContextSupplier());
	}
	
	@Override
	public RxUserSessionSupplier userSessionSupplier(AttributeContext attributeContext) {
		return new RxUserSessionSupplier(() -> attributeContext);
	}

	@Override
	public Supplier<AttributeContext> contextAttributeContextSupplier() {
		return AttributeContexts::peek;
	}

	@Override
	@Managed
	public Supplier<AttributeContext> systemAttributeContextSupplier() {
		return () -> AttributeContexts.derivePeek().set(UserSessionAspect.class, systemUserSession()).build();
	}

	@Managed
	private UserSession systemUserSession() {
		UserSession bean = UserSession.T.create();

		User user = systemUser();

		Set<String> effectiveRoles = new HashSet<>();
		effectiveRoles.add("$all");
		effectiveRoles.add("$user-" + user.getName());
		for (Role userRole : user.getRoles())
			effectiveRoles.add(userRole.getName());

		Date now = new Date();

		bean.setSessionId(UUID.randomUUID().toString());
		bean.setType(UserSessionType.internal);
		bean.setCreationInternetAddress("0:0:0:0:0:0:0:1");
		bean.setCreationDate(now);
		bean.setLastAccessedDate(now);
		bean.setUser(user);
		bean.setEffectiveRoles(effectiveRoles);

		return bean;
	}

	private static final Set<String> internalRoles = set("internal");
	private static final String internalName = "internal";

	@Managed
	private User systemUser() {
		User bean = User.T.create();
		bean.setId(internalName);
		bean.setName(internalName);

		for (String internalRoleName : internalRoles) {
			Role internalRole = Role.T.create();
			internalRole.setId(internalRoleName);
			internalRole.setName(internalRoleName);
			bean.getRoles().add(internalRole);
		}

		return bean;
	}
}