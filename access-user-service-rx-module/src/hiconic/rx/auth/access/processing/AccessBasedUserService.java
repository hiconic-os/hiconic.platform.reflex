package hiconic.rx.auth.access.processing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.gm.initializer.jdbc.processing.GmDbInitializerManager;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.i18n.LocalizedString;
import com.braintribe.model.generic.manipulation.DeleteMode;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.Group;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;

import hiconic.rx.security.api.PasswordHashing;
import hiconic.rx.security.api.UserService;
import hiconic.rx.security.api.UserSessionInvalidation;

/**
 * @author peter.gazdik
 */
public class AccessBasedUserService implements UserService {

	private String authAccessId;
	private PersistenceGmSessionFactory systemSessionFactory;
	private PasswordHashing passwordHashing;
	private UserSessionInvalidation userSessionInvalidation;
	private DataSource provisioningStateDataSource;
	private Locking provisioningLocking;
	private String nodeId;

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

	@Required
	public void setPasswordHashing(PasswordHashing passwordHashing) {
		this.passwordHashing = passwordHashing;
	}

	@Required
	public void setUserSessionInvalidation(UserSessionInvalidation userSessionInvalidation) {
		this.userSessionInvalidation = userSessionInvalidation;
	}

	@Required
	public void setProvisioningStateDataSource(DataSource provisioningStateDataSource) {
		this.provisioningStateDataSource = provisioningStateDataSource;
	}

	@Required
	public void setProvisioningLocking(Locking provisioningLocking) {
		this.provisioningLocking = provisioningLocking;
	}

	@Required
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public String userServiceId() {
		return "access-user-service-" + authAccessId;
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException {
		User user = retrieveUser(userIdentification);

		if (passwordHashing.matches(password, user.getPassword()))
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
		return reconcileUsersInternal(List.of(user), null, false, false);
	}

	@Override
	public Reason reconcileUsers(List<User> users, String provisioningGroup, String reconciliationRevision) {
		return reconcileUsers(users, provisioningGroup, reconciliationRevision, false);
	}

	@Override
	public Reason reconcileUsers(List<User> users, String provisioningGroup, String reconciliationRevision, boolean deleteVanishedUsers) {
		Lock lock = provisioningLocking.forIdentifier("user-provisioning", authAccessId + ":" + provisioningGroup).writeLock();
		lock.lock();
		try {
			return reconcileUsersLocked(users, provisioningGroup, reconciliationRevision, deleteVanishedUsers);
		} finally {
			lock.unlock();
		}
	}

	private Reason reconcileUsersLocked(List<User> users, String provisioningGroup, String reconciliationRevision, boolean deleteVanishedUsers) {
		if (deleteVanishedUsers && (provisioningGroup == null || provisioningGroup.isBlank()))
			return Reasons.build(ConfigurationError.T).text("Cannot delete vanished users without a provisioning group").toReason();

		if (deleteVanishedUsers && (users == null || users.isEmpty()))
			return Reasons.build(ConfigurationError.T)
					.text("Refusing to delete vanished users because the authoritative provisioning source is empty").toReason();

		Reason error = reconcileUsersInternal(users, provisioningGroup, false, true);
		if (error != null)
			return error;

		reconcileVanishedUsers(provisioningGroup, users.stream().map(User::getName).collect(Collectors.toSet()), deleteVanishedUsers);

		if (reconciliationRevision == null || reconciliationRevision.isBlank())
			return null;

		GmDbInitializerManager manager = new GmDbInitializerManager();
		manager.setUseCase("user-provisioning");
		manager.setNodeId(nodeId);
		manager.setDataSource(provisioningStateDataSource);
		manager.setTasksTableName("hc_initializer_tasks");
		manager.setLocking(provisioningLocking);

		String taskName = "credentials:" + provisioningGroup;
		manager.registerInitializer(taskName, oldFingerprint -> reconciliationRevision, () -> {
			Reason credentialError = reconcileUsersInternal(users, provisioningGroup, true, true);
			if (credentialError != null)
				return credentialError.asMaybe();
			return Maybe.complete("Reconciled credentials for " + users.size() + " user(s)");
		});
		manager.runInitializers();

		return null;
	}

	private Reason reconcileUsersInternal(List<User> users, String provisioningGroup, boolean reconcileCredentials, boolean authoritative) {
		PersistenceGmSession session = newSession();
		Group markerGroup = provisioningGroup == null || provisioningGroup.isBlank() ? null : ensureGroup(provisioningGroup, session);

		for (User user : users) {
			String username = user.getName();

			EntityQuery query = EntityQueryBuilder.from(User.T).where().property(User.name).eq(username).tc(everythingExceptGroupUsersTc).done();
			User actualUser = session.query().entities(query).first();
			boolean created = actualUser == null;

			if (created) {
				log.debug(() -> "User " + username + " does not yet exist but we will create it now.");

				actualUser = session.create(User.T);
			}

			copySimplePropsAndLocalizedStrings(user, actualUser, created || authoritative);
			if ((created || reconcileCredentials) && user.getPassword() != null)
				actualUser.setPassword(passwordHashing.hash(user.getPassword()));

			Set<Role> desiredRoles = ensureRoles(user.getRoles(), session);
			Set<String> desiredRoleNames = toRoleNames(desiredRoles);

			// Provisioned roles are authoritative. Runtime role changes must be reflected back into the source.
			actualUser.getRoles().removeIf(r -> !desiredRoleNames.contains(r.getName()));
			actualUser.getRoles().addAll(desiredRoles);

			Set<Group> desiredGroups = ensureGroups(user.getGroups(), session);
			if (markerGroup != null)
				desiredGroups.add(markerGroup);
			reconcileGroups(actualUser, desiredGroups, authoritative);
		}

		session.commit();

		return null;
	}

	private Set<String> toRoleNames(Set<Role> roles) {
		return roles.stream().map(Role::getName).collect(Collectors.toSet());
	}

	private Set<Role> ensureRoles(Set<Role> roles, PersistenceGmSession session) {
		if (roles == null || roles.isEmpty())
			return new HashSet<>();

		Set<String> roleNames = roles.stream().map(Role::getName).filter(n -> n != null).collect(Collectors.toSet());

		List<Role> existingRoles = session.query().entities(EntityQueryBuilder.from(Role.T).where().property(Role.name).in(roleNames).done()).list();
		Map<String, Role> nameToRole = existingRoles.stream()
				.collect(Collectors.toMap(Role::getName, Function.identity(), this::preferExisting));

		Set<Role> result = new HashSet<>(nameToRole.values());
		for (Role role : roles) {
			if (!nameToRole.containsKey(role.getName())) {
				Role newRole = session.create(Role.T);
				copySimplePropsAndLocalizedStrings(role, newRole, true);

				result.add(newRole);
			}
		}

		return result;
	}

	private Set<Group> ensureGroups(Set<Group> groups, PersistenceGmSession session) {
		if (groups == null || groups.isEmpty())
			return new HashSet<>();

		Set<String> groupNames = groups.stream().map(Group::getName).filter(n -> n != null).collect(Collectors.toSet());
		List<Group> existingGroups = session.query().entities(EntityQueryBuilder.from(Group.T).where().property(Group.name).in(groupNames).done()).list();
		Map<String, Group> nameToGroup = existingGroups.stream()
				.collect(Collectors.toMap(Group::getName, Function.identity(), this::preferExisting));
		Set<Group> result = new HashSet<>(nameToGroup.values());

		for (Group group : groups) {
			if (!nameToGroup.containsKey(group.getName())) {
				Group newGroup = session.create(Group.T);
				copySimplePropsAndLocalizedStrings(group, newGroup, true);
				result.add(newGroup);
			}
		}

		return result;
	}

	private <E extends GenericEntity> E preferExisting(E first, E duplicate) {
		log.warn("Multiple persisted entities share the same provisioning identity; retaining " + first + " and ignoring " + duplicate);
		return first;
	}

	private Group ensureGroup(String groupName, PersistenceGmSession session) {
		Group group = session.query().entities(EntityQueryBuilder.from(Group.T).where().property(Group.name).eq(groupName).done()).first();
		if (group == null) {
			group = session.create(Group.T);
			group.setName(groupName);
		}
		return group;
	}

	private void reconcileGroups(User user, Set<Group> desiredGroups, boolean authoritative) {
		if (authoritative) {
			for (Group group : new ArrayList<>(user.getGroups())) {
				if (!desiredGroups.contains(group)) {
					user.getGroups().remove(group);
					group.getUsers().remove(user);
				}
			}
		}

		for (Group group : desiredGroups) {
			user.getGroups().add(group);
			group.getUsers().add(user);
		}
	}

	private void reconcileVanishedUsers(String provisioningGroup, Set<String> desiredUserNames, boolean deleteVanishedUsers) {
		if (provisioningGroup == null || provisioningGroup.isBlank())
			return;

		PersistenceGmSession session = newSession();
		Group attachedMarker = session.query().entities(EntityQueryBuilder.from(Group.T).where().property(Group.name).eq(provisioningGroup).done()).first();
		if (attachedMarker == null)
			return;

		List<User> ownedUsers = session.query().entities(EntityQueryBuilder.from(User.T).where().property(User.groups).contains()
				.entity(attachedMarker).tc(everythingExceptGroupUsersTc).done()).list();
		List<User> vanishedUsers = ownedUsers.stream().filter(user -> !desiredUserNames.contains(user.getName())).toList();
		if (vanishedUsers.isEmpty())
			return;

		List<String> vanishedNames = vanishedUsers.stream().map(User::getName).sorted().toList();
		if (!deleteVanishedUsers) {
			log.warn("Provisioned users no longer present in the source are retained: " + vanishedNames);
			return;
		}

		int invalidatedSessions = 0;
		for (User vanishedUser : vanishedUsers) {
			invalidatedSessions += userSessionInvalidation.invalidateUserSessions(vanishedUser.getName());
			session.deleteEntity(vanishedUser, DeleteMode.dropReferences);
		}
		session.commit();
		log.info("Deleted users vanished from provisioning source [" + provisioningGroup + "]: " + vanishedNames
				+ "; invalidated " + invalidatedSessions + " session(s)");
	}

	private <E extends GenericEntity> void copySimplePropsAndLocalizedStrings(E source, E target, boolean overwrite) {
		for (Property p : source.entityType().getProperties()) {
			if (p.getType().isSimple()) {
				if (!p.isIdentifier() && !p.isPartition() && !p.isGlobalId() && !User.password.equals(p.getName())
						&& (overwrite || p.get(target) == null))
					p.set(target, p.get(source));
				continue;
			}

			if (LocalizedString.T.isAssignableFrom(p.getType())) {
				LocalizedString ls = p.get(source);
				if (ls != null) {
					LocalizedString newLs = target.session().create(LocalizedString.T);
					newLs.setLocalizedValues(ls.getLocalizedValues());
					p.set(target, newLs);
				}
			}
		}
	}

	private PersistenceGmSession newSession() {
		return systemSessionFactory.newSession(authAccessId);
	}

}
