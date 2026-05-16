package hiconic.rx.auth.access.processing;

import static com.braintribe.utils.lcd.CollectionTools2.index;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.i18n.LocalizedString;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.Group;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;

import hiconic.rx.security.api.UserService;

/**
 * @author peter.gazdik
 */
public class AccessBasedUserService implements UserService {

	private String authAccessId;
	private PersistenceGmSessionFactory systemSessionFactory;

	private static final Logger log = Logger.getLogger(AccessBasedUserService.class);

	// @formatter:off
	private static TraversingCriterion everythingExceptGroupUsersTc = TC.create()
			.pattern()
				.entity(Group.T)
				.property(Group.users)
			.close()
			.done();
	// @formatter:on

	@Required
	public void setAuthAccessId(String authAccessId) {
		this.authAccessId = authAccessId;
	}

	@Required
	public void setSystemSessionFactory(PersistenceGmSessionFactory systemSessionFactory) {
		this.systemSessionFactory = systemSessionFactory;
	}

	@Override
	public String userServiceId() {
		return "access-user-service-" + authAccessId;
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException {
		User user = retrieveUser(userIdentification);

		// TODO handle password hashing etc.
		if (password.equals(user.getPassword()))
			return user;

		throw new UserNotFoundException();
	}

	@Override
	public User findUser(String propertyName, String propertyValue) {
		EntityQuery query = EntityQueryBuilder.from(User.T).where().property(propertyName).eq(propertyValue).tc(everythingExceptGroupUsersTc).done();
		PersistenceGmSession session = newSession();

		return session.queryDetached().entities(query).first();
	}

	@Override
	public Reason ensureUser(User user) {
		PersistenceGmSession session = newSession();

		String username = user.getName();

		EntityQuery query = EntityQueryBuilder.from(User.T).where().property(User.name).eq(username).tc(everythingExceptGroupUsersTc).done();
		User actualUser = session.query().entities(query).first();

		if (actualUser == null) {
			log.debug(() -> "User " + username + " does not yet exist but we will create it now.");

			actualUser = session.create(User.T);
			copySimplePropsAndLocalizedStrings(user, actualUser);
		}

		Set<Role> desiredRoles = ensureRoles(user.getRoles(), session);

		Set<String> desiredRoleNames = toRoleNamesf(desiredRoles);

		// remove roles no longer relevant
		actualUser.getRoles().removeIf(r -> !desiredRoleNames.contains(r.getName()));

		// add missing desired roles
		actualUser.getRoles().addAll(desiredRoles);

		session.commit();

		return null;
	}

	private Set<String> toRoleNamesf(Set<Role> roles) {
		return roles.stream().map(Role::getName).collect(Collectors.toSet());
	}

	private Set<Role> ensureRoles(Set<Role> roles, PersistenceGmSession session) {
		Set<String> roleNames = roles.stream().map(Role::getName).filter(n -> n != null).collect(Collectors.toSet());

		List<Role> existingRoles = session.query().entities(EntityQueryBuilder.from(Role.T).where().property(Role.name).in(roleNames).done()).list();
		Map<String, Role> nameToRole = index(existingRoles).by(Role::getName).unique();

		Set<Role> result = new HashSet<>(existingRoles);
		for (Role role : roles) {
			if (!nameToRole.containsKey(role.getName())) {
				Role newRole = session.create(Role.T);
				copySimplePropsAndLocalizedStrings(role, newRole);

				result.add(newRole);
			}
		}

		return result;
	}

	private <E extends GenericEntity> void copySimplePropsAndLocalizedStrings(E source, E target) {
		for (Property p : source.entityType().getProperties()) {
			if (p.getType().isSimple()) {
				if (!p.isPartition() && !p.isGlobalId())
					p.set(target, p.get(source));
				continue;
			}

			if (LocalizedString.T.isAssignableFrom(p.getType())) {
				LocalizedString ls = p.get(source);
				if (ls != null) {
					LocalizedString newLs = target.session().create(LocalizedString.T);
					newLs.setLocalizedValues(ls.getLocalizedValues());
				}
			}
		}
	}

	private PersistenceGmSession newSession() {
		return systemSessionFactory.newSession(authAccessId);
	}

}
