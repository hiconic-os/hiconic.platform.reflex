// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.braintribe.cfg.Required;
import com.braintribe.model.folder.Folder;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.InstantiationManipulation;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.ManipulationType;
import com.braintribe.model.generic.tracking.ManipulationListener;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.workbench.KnownWorkenchPerspective;
import com.braintribe.model.workbench.WorkbenchConfiguration;
import com.braintribe.model.workbench.WorkbenchPerspective;

import hiconic.rx.workbench.api.WorkbenchInitializer;
import hiconic.rx.workbench.api.WorkbenchInitializerIdentity;
import hiconic.rx.workbench.api.WorkbenchInitializerRegistration;

public class WorkbenchInitializers {

	private record RegisteredInitializer(WorkbenchInitializer initializer, WorkbenchInitializerIdentity identity) {}

	private final Map<String, List<RegisteredInitializer>> initializers = new LinkedHashMap<>();
	private PersistenceGmSessionFactory sessionFactory;
	private WorkbenchInitializerIdentity standardInitializerIdentity;

	@Required
	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Required
	public void setStandardInitializerIdentity(WorkbenchInitializerIdentity standardInitializerIdentity) {
		this.standardInitializerIdentity = standardInitializerIdentity;
	}

	public synchronized void register(String accessId, WorkbenchInitializer initializer) {
		initializers.computeIfAbsent(accessId, ignored -> new ArrayList<>()).add(new RegisteredInitializer(initializer, null));
	}

	public synchronized void register(String accessId, WorkbenchInitializerRegistration registration) {
		List<RegisteredInitializer> accessInitializers = initializers.computeIfAbsent(accessId, ignored -> new ArrayList<>());
		String prefix = registration.identity().prefix();
		if (standardInitializerIdentity.prefix().equals(prefix)
				|| accessInitializers.stream().map(RegisteredInitializer::identity).filter(identity -> identity != null)
						.anyMatch(identity -> identity.prefix().equals(prefix)))
			throw new IllegalStateException("Duplicate stable workbench initializer identity for access '" + accessId + "': " + prefix);

		accessInitializers.add(new RegisteredInitializer(registration.initializer(), registration.identity()));
	}

	public void initializeAll() {
		Map<String, List<RegisteredInitializer>> snapshot;
		synchronized (this) {
			snapshot = new LinkedHashMap<>(initializers);
		}

		for (Map.Entry<String, List<RegisteredInitializer>> entry : snapshot.entrySet()) {
			PersistenceGmSession session = sessionFactory.newSession(entry.getKey());
			initializeWithStableIds(session, standardInitializerIdentity, () -> ensureStandardWorkbench(session));
			// The standard stage is a predecessor of all contributed initializers. Commit it so their
			// access queries can resolve the entities just established by that stage.
			session.commit();
			entry.getValue().forEach(initializer -> initializeWithStableIds(session, initializer.identity(), () -> initializer.initializer().initialize(session)));
			session.commit();
		}
	}

	private void initializeWithStableIds(PersistenceGmSession session, WorkbenchInitializerIdentity identity, Runnable initializer) {
		if (identity == null) {
			initializer.run();
			return;
		}

		StableIdAssigner assigner = new StableIdAssigner(identity.prefix());
		session.listeners().add(assigner);
		try {
			initializer.run();
			assigner.assign();
		} finally {
			session.listeners().remove(assigner);
		}
	}

	private static class StableIdAssigner implements ManipulationListener {
		private final String prefix;
		private final List<GenericEntity> entities = new ArrayList<>();

		private StableIdAssigner(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public void noticeManipulation(Manipulation manipulation) {
			if (manipulation.manipulationType() == ManipulationType.INSTANTIATION)
				entities.add(((InstantiationManipulation) manipulation).getEntity());
		}

		private void assign() {
			int index = 0;
			for (GenericEntity entity : entities) {
				if (entity.getId() != null)
					continue;

				String id = prefix + "." + index++;
				entity.setId(id);
				if (entity.getGlobalId() == null)
					entity.setGlobalId(id);
			}
		}
	}

	private void ensureStandardWorkbench(PersistenceGmSession session) {
		if (session.query().entities(EntityQueryBuilder.from(WorkbenchConfiguration.T).done()).first() == null)
			session.create(WorkbenchConfiguration.T).setLocale("auto");

		Map<String, Folder> perspectiveRoots = new LinkedHashMap<>();
		Arrays.asList( //
				KnownWorkenchPerspective.root, //
				KnownWorkenchPerspective.homeFolder, //
				KnownWorkenchPerspective.actionBar, //
				KnownWorkenchPerspective.headerBar, //
				KnownWorkenchPerspective.globalActionBar, //
				KnownWorkenchPerspective.tabActionBar) //
				.forEach(perspective -> perspectiveRoots.put(perspective.toString(), ensurePerspective(session, perspective.toString())));

		// These names are part of the Explorer/Workbench protocol.  They are not
		// application customization: the client uses them as slots for its built-in
		// actions and controls (most notably the quick access/search control).
		List.of( //
				"actionbar/$exchangeContentView", //
				"actionbar/$workWithEntity", //
				"actionbar/$gimaOpener", //
				"actionbar/$deleteEntity", //
				"actionbar/$changeInstance", //
				"actionbar/$clearEntityToNull", //
				"actionbar/$addToCollection", //
				"actionbar/$insertBeforeToList", //
				"actionbar/$removeFromCollection", //
				"actionbar/$clearCollection", //
				"actionbar/$refreshEntities", //
				"actionbar/$ResourceDownload", //
				"actionbar/$executeServiceRequest", //
				"actionbar/$addToClipboard", //
				"headerbar/tb_Logo", //
				"headerbar/$quickAccess-slot", //
				"headerbar/$globalState-slot", //
				"headerbar/$settingsMenu/$reloadSession", //
				"headerbar/$settingsMenu/$showSettings", //
				"headerbar/$settingsMenu/$uiTheme", //
				"headerbar/$settingsMenu/$showAbout", //
				"headerbar/$userMenu/$showUserProfile", //
				"headerbar/$userMenu/$showLogout", //
				"tab-actionbar/$explorer/$homeConstellation", //
				"tab-actionbar/$explorer/$changesConstellation", //
				"tab-actionbar/$explorer/$transientChangesConstellation", //
				"tab-actionbar/$explorer/$clipboardConstellation", //
				"tab-actionbar/$explorer/$notificationsConstellation", //
				"tab-actionbar/$selection/$homeConstellation", //
				"tab-actionbar/$selection/$changesConstellation", //
				"tab-actionbar/$selection/$transientChangesConstellation", //
				"tab-actionbar/$selection/$clipboardConstellation", //
				"tab-actionbar/$selection/$quickAccessConstellation", //
				"tab-actionbar/$selection/$expertUI", //
				"global-actionbar/$new", //
				"global-actionbar/$dualSectionButtons", //
				"global-actionbar/$upload", //
				"global-actionbar/$undo", //
				"global-actionbar/$redo", //
				"global-actionbar/$commit") //
				.forEach(path -> ensureFolderPath(session, perspectiveRoots, path));
	}

	private void ensureFolderPath(PersistenceGmSession session, Map<String, Folder> perspectiveRoots, String path) {
		String[] names = path.split("/");
		Folder parent = perspectiveRoots.get(names[0]);
		for (int i = 1; i < names.length; i++) {
			String name = names[i];
			Folder child = parent.getSubFolders().stream().filter(folder -> name.equals(folder.getName())).findFirst().orElse(null);
			if (child == null) {
				child = session.create(Folder.T).initFolder(name, displayName(name));
				child.setParent(parent);
				parent.getSubFolders().add(child);
			}
			parent = child;
		}
	}

	private Folder ensurePerspective(PersistenceGmSession session, String name) {
		WorkbenchPerspective existing = session.query().entities(EntityQueryBuilder.from(WorkbenchPerspective.T).where().property("name").eq(name).done())
				.first();
		if (existing != null && !existing.getFolders().isEmpty())
			return existing.getFolders().get(0);

		Folder root = session.create(Folder.T).initFolder(name, displayName(name));
		WorkbenchPerspective perspective = existing != null ? existing
				: session.create(WorkbenchPerspective.T).initWorkbenchPerspective(name, displayName(name));
		perspective.getFolders().add(root);
		return root;
	}

	private String displayName(String name) {
		return switch (name) {
			case "root" -> "Root";
			case "homeFolder" -> "Home";
			case "actionbar" -> "Action Bar";
			case "headerbar" -> "Header Bar";
			case "global-actionbar" -> "Global Action Bar";
			case "tab-actionbar" -> "Tab Action Bar";
			default -> name.startsWith("$") ? name.substring(1) : name;
		};
	}

}
