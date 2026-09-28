// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.explorer.wire.space;

import java.util.List;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.scope.InstanceConfiguration;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.smood.model.configuration.SmoodAccess;
import hiconic.rx.explorer.processing.SystemWorkbenchInitializer;
import hiconic.rx.explorer.processing.SystemWorkbenchInitializer.Entry;
import hiconic.rx.module.api.wire.ModuleReflectionContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.workbench.api.WorkbenchContract;
import hiconic.rx.workbench.api.WorkbenchInitializerIdentity;
import hiconic.rx.workbench.api.WorkbenchInitializerRegistration;

/** Defines the system-access workbenches exposed by RX Explorer. */
@Managed
public class SystemWorkbenchesSpace implements WireSpace {

	private static final String AUTH_ACCESS = "auth";
	private static final String USER_SESSIONS_ACCESS = "user-sessions";
	private static final String META_WORKBENCH_ACCESS = "workbench";
	private static final String CORTEX_WORKBENCH_ACCESS = CortexSpace.CORTEX_ACCESS_ID + ".wb";
	private static final String AUTH_WORKBENCH_ACCESS = AUTH_ACCESS + ".wb";
	private static final String USER_SESSIONS_WORKBENCH_ACCESS = USER_SESSIONS_ACCESS + ".wb";

	private static final List<String> WORKBENCH_MODELS = List.of(
			"com.braintribe.gm:legacy-workbench-model",
			"com.braintribe.gm:essential-meta-data-model",
			"com.braintribe.gm:basic-value-descriptor-model");
	private static final List<Integer> ICON_SIZES = List.of(16, 24, 32, 64);
	private static final List<Integer> SMALL_AND_LARGE_ICON_SIZES = List.of(16, 64);

	@Import private AccessContract access;
	@Import private WorkbenchContract workbenches;
	@Import private ModuleReflectionContract moduleReflection;
	@Import private RxPlatformContract platform;

	public void configureModels() {
		access.configureModels(metaWorkbench());
		access.configureModels(cortexWorkbench());
		access.configureModels(authWorkbench());
		access.configureModels(userSessionsWorkbench());
	}

	public void deployAccesses() {
		deploy(metaWorkbench());
		deploy(cortexWorkbench());
		deployIfPresent(AUTH_ACCESS, authWorkbench());
		deployIfPresent(USER_SESSIONS_ACCESS, userSessionsWorkbench());
	}

	public void registerWorkbenches() {
		register(META_WORKBENCH_ACCESS, META_WORKBENCH_ACCESS, metaWorkbenchEntries(), List.of("templates"));

		register(CortexSpace.CORTEX_ACCESS_ID, CORTEX_WORKBENCH_ACCESS, List.of(
				new Entry("accesses", "Accesses", "hiconic.rx.explorer.model.cortex.access.RxIncrementalAccess")));
		workbenches.associateWorkbench(CORTEX_WORKBENCH_ACCESS, META_WORKBENCH_ACCESS);

		registerIfPresent(AUTH_ACCESS, AUTH_WORKBENCH_ACCESS, authWorkbenchEntries());
		if (access.accessDomains().hasDomain(AUTH_WORKBENCH_ACCESS))
			workbenches.associateWorkbench(AUTH_WORKBENCH_ACCESS, META_WORKBENCH_ACCESS);

		registerIfPresent(USER_SESSIONS_ACCESS, USER_SESSIONS_WORKBENCH_ACCESS, List.of(
				new Entry("user-sessions", "User Sessions", "com.braintribe.gm.model.usersession.PersistenceUserSession")));
		if (access.accessDomains().hasDomain(USER_SESSIONS_WORKBENCH_ACCESS))
			workbenches.associateWorkbench(USER_SESSIONS_WORKBENCH_ACCESS, META_WORKBENCH_ACCESS);
	}

	private void registerIfPresent(String dataAccessId, String workbenchAccessId, List<Entry> entries) {
		if (access.accessDomains().hasDomain(dataAccessId))
			register(dataAccessId, workbenchAccessId, entries);
	}

	private void register(String dataAccessId, String workbenchAccessId, List<Entry> entries) {
		register(dataAccessId, workbenchAccessId, entries, List.of());
	}

	private void register(String dataAccessId, String workbenchAccessId, List<Entry> entries, List<String> obsoleteRootEntries) {
		workbenches.registerWorkbench(dataAccessId, workbenchAccessId,
				new WorkbenchInitializerRegistration(
						new SystemWorkbenchInitializer(entries, obsoleteRootEntries, this::workbenchResource), structuredInitializerIdentity()));
	}

	private com.braintribe.model.resource.Resource workbenchResource(String path) {
		return platform.packagedResources().resolver().resource("explorer-webpages/workbench/" + path)
				.withMimeType().withFileSize().withSpecification().asResource();
	}

	private List<Entry> authWorkbenchEntries() {
		return List.of(
				Entry.folder("user-administration", "User Administration",
						Entry.folder("users-groups", "Users & Groups",
								Entry.query("all-identities", "All Identities", "com.braintribe.model.user.Identity",
										"auth/us_identities", ICON_SIZES, true),
								Entry.query("users", "Users", "com.braintribe.model.user.User", "auth/us_user", ICON_SIZES, true),
								Entry.query("groups", "Groups", "com.braintribe.model.user.Group", "auth/us_group", ICON_SIZES, true)),
						Entry.folder("authorization", "Authorization",
								Entry.query("roles", "Roles", "com.braintribe.model.user.Role", "auth/us_role", ICON_SIZES, true))),
				Entry.folder("system", "System",
						Entry.folder("resources", "Resources",
								Entry.likeQuery("images", "Images", "com.braintribe.model.resource.Resource",
										"auth/resource_image", SMALL_AND_LARGE_ICON_SIZES, true, "mimeType", "image*"),
								Entry.query("files", "Files", "com.braintribe.model.resource.Resource",
										"auth/resource_file", SMALL_AND_LARGE_ICON_SIZES, true))));
	}

	private List<Entry> metaWorkbenchEntries() {
		return List.of(
				Entry.folder("workbench-administration", "Workbench Administration",
						Entry.folder("gme-configuration", "GME Configuration",
								Entry.queryFolder("perspectives", "Perspectives", "com.braintribe.model.workbench.WorkbenchPerspective",
										perspective("entry-points", "Entry Points", "root", "meta/wb_entrypoints", true),
										perspective("home-screen", "Home Screen", "homeFolder", "meta/wb_home", true),
										perspective("action-bar", "Action Bar", "actionbar", "meta/wb_actionbar", true),
										perspective("global-action-bar", "Global Action Bar", "global-actionbar", "meta/wb_actionbar", false),
										perspective("header-bar", "Header Bar", "headerbar", "meta/wb_actionbar", false),
										perspective("tab-action-bar", "Tab Action Bar", "tab-actionbar", "meta/wb_actionbar", false)),
								Entry.folder("folders", "Folders",
										Entry.query("all-folders", "All Folders", "com.braintribe.model.folder.Folder",
												"meta/wb_allfolder", ICON_SIZES, true),
										Entry.filteredQuery("top-level-folders", "Top-level Folders", "com.braintribe.model.folder.Folder",
												"meta/wb_allfolder", ICON_SIZES, false, "parent", null, false)),
								Entry.queryFolder("actions", "Actions", "com.braintribe.model.workbench.WorkbenchAction",
										Entry.filteredQuery("context-sensitive", "Context Sensitive", "com.braintribe.model.workbench.WorkbenchAction",
												"meta/wb_action", SMALL_AND_LARGE_ICON_SIZES, false, "inplaceContextCriterion", null, true),
										Entry.filteredQuery("global", "Global", "com.braintribe.model.workbench.WorkbenchAction",
												"meta/wb_action", SMALL_AND_LARGE_ICON_SIZES, true, "inplaceContextCriterion", null, false)),
								Entry.folder("styling", "Styling",
										Entry.query("configuration", "Configuration", "com.braintribe.model.workbench.WorkbenchConfiguration",
												null, List.of(), false)))),
				Entry.folder("system", "System",
						Entry.folder("resources", "Resources",
								Entry.likeQuery("images", "Images", "com.braintribe.model.resource.Resource",
										"meta/resource_image", SMALL_AND_LARGE_ICON_SIZES, true, "mimeType", "image*"),
								Entry.query("files", "Files", "com.braintribe.model.resource.Resource",
										"meta/resource_file", SMALL_AND_LARGE_ICON_SIZES, false))));
	}

	private Entry perspective(String name, String displayName, String perspectiveName, String iconName, boolean home) {
		return Entry.filteredQuery(name, displayName, "com.braintribe.model.workbench.WorkbenchPerspective",
				iconName, ICON_SIZES, home, "name", perspectiveName, false);
	}

	@Managed
	private WorkbenchInitializerIdentity structuredInitializerIdentity() {
		return WorkbenchInitializerIdentity.wire(moduleReflection, InstanceConfiguration.currentInstance().qualification());
	}

	private void deploy(SmoodAccess denotation) {
		access.protectSystemAccess(denotation.getAccessId());
		access.deploy(denotation);
	}

	private void deployIfPresent(String dataAccessId, SmoodAccess workbenchDenotation) {
		if (access.accessDomains().hasDomain(dataAccessId))
			deploy(workbenchDenotation);
	}

	@Managed
	private SmoodAccess cortexWorkbench() {
		return workbenchAccess(CORTEX_WORKBENCH_ACCESS, "RX Cortex Workbench");
	}

	@Managed
	private SmoodAccess metaWorkbench() {
		return workbenchAccess(META_WORKBENCH_ACCESS, "Workbench");
	}

	@Managed
	private SmoodAccess authWorkbench() {
		return workbenchAccess(AUTH_WORKBENCH_ACCESS, "Authentication and Authorization Workbench");
	}

	@Managed
	private SmoodAccess userSessionsWorkbench() {
		return workbenchAccess(USER_SESSIONS_WORKBENCH_ACCESS, "User Sessions Workbench");
	}

	private SmoodAccess workbenchAccess(String accessId, String displayName) {
		SmoodAccess result = SmoodAccess.T.create();
		result.setAccessId(accessId);
		result.setDisplayName(displayName);
		result.getDataModelNames().addAll(WORKBENCH_MODELS);
		return result;
	}
}
