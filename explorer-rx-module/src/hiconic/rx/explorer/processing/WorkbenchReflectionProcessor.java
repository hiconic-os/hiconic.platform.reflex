package hiconic.rx.explorer.processing;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.persistence.reflection.api.GetModelAndWorkbenchEnvironment;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.logging.Logger;
import com.braintribe.model.access.ModelAccessException;
import com.braintribe.model.accessapi.ModelEnvironment;
import com.braintribe.model.acl.AclOperation;
import com.braintribe.model.folder.Folder;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.workbench.WorkbenchConfiguration;
import com.braintribe.model.workbench.WorkbenchPerspective;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.explorer.model.configuration.ExplorerConfiguration;
import hiconic.rx.explorer.model.configuration.ModelEnvironmentConfiguration;
import hiconic.rx.module.api.service.ConfiguredModel;

/** Explorer-specific composition of data, service and workbench model environments. */
public class WorkbenchReflectionProcessor implements ReasonedServiceProcessor<GetModelAndWorkbenchEnvironment, ModelEnvironment> {

	private static final Logger log = Logger.getLogger(WorkbenchReflectionProcessor.class);
	private static final Set<String> WORKBENCH_ROOT_FOLDER_NAMES = Set.of(
			"root", "actionbar", "headerbar", "homeFolder", "tab-actionbar", "global-actionbar");

	private AccessDomains accesses;
	private PersistenceGmSessionFactory sessionFactory;
	private final Map<String, String> workbenchAccessIds = new HashMap<>();

	@Required
	public void setAccesses(AccessDomains accesses) {
		this.accesses = accesses;
	}

	@Required
	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Required
	public void setConfiguration(ExplorerConfiguration configuration) {
		for (ModelEnvironmentConfiguration environment : configuration.getModelEnvironments()) {
			String previous = workbenchAccessIds.put(environment.getDataAccessId(), environment.getWorkbenchAccessId());
			if (previous != null)
				throw new IllegalStateException("Multiple Explorer model environments configured for access: " + environment.getDataAccessId());
		}
	}

	@Override
	public Maybe<? extends ModelEnvironment> processReasoned(ServiceRequestContext context, GetModelAndWorkbenchEnvironment request) {
		return modelEnvironment(request.getAccessId(), request.getFoldersByPerspective());
	}

	private Maybe<ModelEnvironment> modelEnvironment(String accessId, Set<String> perspectiveNames) {
		if (accessId == null)
			return Reasons.build(InvalidArgument.T).text("Access id must not be null").toMaybe();

		AccessDomain dataAccess = accesses.byId(accessId);
		if (dataAccess == null)
			return Reasons.build(NotFound.T).text("No access found for id: " + accessId).toMaybe();

		ConfiguredModel dataModel = dataAccess.configuredDataModel();
		ConfiguredModel serviceModel = dataAccess.configuredServiceModel();
		ModelEnvironment result = ModelEnvironment.T.create();
		result.setDataAccessId(accessId);
		result.setDataModel(dataModel.modelOracle().getGmMetaModel());
		result.setServiceModel(serviceModel.modelOracle().getGmMetaModel());
		result.setServiceModelName(serviceModel.name());

		String workbenchAccessId = workbenchAccessIds.get(accessId);
		if (StringTools.isBlank(workbenchAccessId))
			return Maybe.complete(result);

		AccessDomain workbenchAccess = accesses.byId(workbenchAccessId);
		if (workbenchAccess == null)
			return Reasons.build(NotFound.T)
					.text("Workbench access '" + workbenchAccessId + "' configured for access '" + accessId + "' was not found")
					.toMaybe();

		result.setWorkbenchModelAccessId(workbenchAccessId);
		result.setWorkbenchModel(workbenchAccess.configuredDataModel().modelOracle().getGmMetaModel());
		try {
			UserSession userSession = currentUserSession();
			if (perspectiveNames == null || perspectiveNames.isEmpty()) {
				Set<Folder> rootFolders = queryRootFolders(workbenchAccessId);
				filterUnreadableFolders(rootFolders, userSession);
				result.setWorkbenchRootFolders(rootFolders);
			} else {
				List<WorkbenchPerspective> perspectives = queryPerspectives(workbenchAccessId, perspectiveNames);
				for (WorkbenchPerspective perspective : perspectives)
					filterUnreadableFolders(perspective.getFolders(), userSession);
				result.setPerspectives(perspectives);
			}
			result.setWorkbenchConfiguration(queryConfiguration(workbenchAccessId));
		} catch (ModelAccessException e) {
			return Reasons.build(InternalError.T)
					.text("Could not read workbench access '" + workbenchAccessId + "' for access '" + accessId + "'")
					.enrich(reason -> reason.setJavaException(e))
					.toMaybe();
		}

		applySessionLocale(result.getWorkbenchConfiguration());
		return Maybe.complete(result);
	}

	private Set<Folder> queryRootFolders(String workbenchAccessId) throws ModelAccessException {
		PersistenceGmSession session = sessionFactory.newSession(workbenchAccessId);
		hydrateFolders(session);
		EntityQuery query = EntityQueryBuilder.from(Folder.T).where().property(Folder.name).in(WORKBENCH_ROOT_FOLDER_NAMES)
				.tc(workbenchTraversingCriterion()).done();
		query.setNoAbsenceInformation(true);
		return new HashSet<>(session.query().entities(query).list());
	}

	private WorkbenchConfiguration queryConfiguration(String workbenchAccessId) throws ModelAccessException {
		PersistenceGmSession session = sessionFactory.newSession(workbenchAccessId);
		return session.query().entities(EntityQueryBuilder.from(WorkbenchConfiguration.T)
				.tc(workbenchTraversingCriterion()).done()).first();
	}

	private List<WorkbenchPerspective> queryPerspectives(String workbenchAccessId, Set<String> names) throws ModelAccessException {
		PersistenceGmSession session = sessionFactory.newSession(workbenchAccessId);
		hydrateFolders(session);
		EntityQuery query = EntityQueryBuilder.from(WorkbenchPerspective.T).where().property("name").in(names)
				.tc(workbenchTraversingCriterion()).done();
		query.setNoAbsenceInformation(true);
		return session.query().entities(query).list();
	}

	private void hydrateFolders(PersistenceGmSession session) throws ModelAccessException {
		// Perspective queries return their nested Folder references, but ordinary
		// accesses do not necessarily materialize the referenced folders' complete
		// graphs. Loading the small workbench folder inventory in the same session
		// makes icons, actions and their resource sources part of the environment.
		session.query().entities(EntityQueryBuilder.from(Folder.T)
				.tc(workbenchTraversingCriterion()).done()).list();
	}

	private void applySessionLocale(WorkbenchConfiguration configuration) {
		if (configuration == null || !"auto".equalsIgnoreCase(configuration.getLocale()))
			return;
		UserSession userSession = currentUserSession();
		if (userSession != null && !StringTools.isBlank(userSession.locale())) {
			log.trace(() -> "Setting locale " + userSession.locale() + " for session " + userSession.getSessionId());
			configuration.setLocale(userSession.locale());
		}
	}

	private UserSession currentUserSession() {
		return AttributeContexts.peek().findOrNull(UserSessionAspect.class);
	}

	private void filterUnreadableFolders(java.util.Collection<Folder> folders, UserSession userSession) {
		String userName = userSession == null || userSession.getUser() == null ? null : userSession.getUser().getName();
		Set<String> roles = userSession == null || userSession.getEffectiveRoles() == null
				? Collections.emptySet()
				: userSession.getEffectiveRoles();
		Set<Folder> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		folders.removeIf(folder -> !isReadable(folder, userName, roles));
		for (Folder folder : folders)
			filterUnreadableChildren(folder, userName, roles, visited);
	}

	private void filterUnreadableChildren(Folder folder, String userName, Set<String> roles, Set<Folder> visited) {
		if (!visited.add(folder))
			return;
		folder.getSubFolders().removeIf(child -> !isReadable(child, userName, roles));
		for (Folder child : folder.getSubFolders())
			filterUnreadableChildren(child, userName, roles, visited);
	}

	private boolean isReadable(Folder folder, String userName, Set<String> roles) {
		return folder.isOperationGranted(AclOperation.READ, userName, roles);
	}

	private static TraversingCriterion workbenchTraversingCriterion() {
		return TC.create().negation().joker().done();
	}
}
