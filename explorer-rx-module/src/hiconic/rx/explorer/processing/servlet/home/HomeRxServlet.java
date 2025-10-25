// ============================================================================
package hiconic.rx.explorer.processing.servlet.home;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.meta.data.prompt.Visible;
import com.braintribe.model.packaging.Packaging;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.explorer.home.model.Home;
import hiconic.rx.explorer.home.model.Link;
import hiconic.rx.explorer.home.model.LinkCollection;
import hiconic.rx.explorer.home.model.LinkGroup;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.servlet.velocity.BasicTemplateBasedServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Main page with links to the most common needed resources (e.g., Explorer, About page, Logs,...).
 * <p>
 * The page is aware of the user logged in. If no user is logged in, the page is nearly empty, save for the login button.
 */
public class HomeRxServlet extends BasicTemplateBasedServlet {

	private static final long serialVersionUID = 4695919181704450507L;
	private static final String templateName = "home.html.vm";

	private static final Logger logger = Logger.getLogger(HomeRxServlet.class);

	private static final String USECASE_GME_LOGON = "clientLogon";

	public HomeRxServlet() {
		this.refreshFileBasedTemplates = true;
	}

	private ServiceDomains serviceDomains;
	private AccessDomains accessDomains;

	private final List<BiConsumer<? super ServiceDomain, LinkCollection>> serviceDomainLinkConfigurers = newList();
	private final List<BiConsumer<? super AccessDomain, LinkCollection>> accessLinkConfigurers = newList();

	/* Packaging */
	private Supplier<Packaging> packagingProvider = () -> null;

	/* Privileges */
	private Set<String> grantedRoles = Collections.singleton("tf-admin");

	/* Application Links */
	private String explorerUrlWithTrailingSlash;

	/* Relative Paths */
	private String relativeLogPath = "logs";
	private String relativeAboutPath = "about";
	private String relativeDeploymentSummaryPath = "deployment-summary";

	// TODO user proper URLs for RunCheckBundles and and RunDistributedCheckBundles with HTML marshalling
	// TODO register HTML marshaller
	private final String relativePlatformBaseChecksPath = "api/v1/checkPlatform";
	private final String relativePlatformChecksPath = "api/v1/check";
	private String relativeSignInPath = "login";

	/* Static Links */
	private String applicationName;
	private String onlineCompanyUrl;
	private String onlineCompanyImageUrl;
	/* Default AccessIds */
	private String defaultAuthAccessId = "auth";
	private String defaultUserSessionAccessId = "user-sessions";
	private String defaultUserStatisticsAccessId = "user-statistics";
	private String defaultUserSetupAccessId = "setup";

	@SuppressWarnings("unused")
	private Evaluator<ServiceRequest> systemServiceRequestEvaluator;

	@Override
	public void init() {
		setRelativeTemplateLocation(templateName);
	}

	// @formatter:off
	@Required public void setServiceDomains(ServiceDomains serviceDomains) { this.serviceDomains = serviceDomains; }
	@Required public void setAccessDomains(AccessDomains accessDomains) { this.accessDomains = accessDomains; }

	/* Privileges */
	public void setGrantedRoles(Set<String> grantedRoles) { this.grantedRoles = grantedRoles; }
	public void setPackagingProvider(Supplier<Packaging> packagingProvider) { this.packagingProvider = packagingProvider; }

	/* Default AccessIds */
	public void setDefaultAuthAccessId(String defaultAuthAccessId) { this.defaultAuthAccessId = defaultAuthAccessId; }
	public void setDefaultUserSessionAccessId(String defaultUserSessionAccessId) { this.defaultUserSessionAccessId = defaultUserSessionAccessId; }
	public void setDefaultUserStatisticsAccessId(String defaultUserStatisticsAccessId) { this.defaultUserStatisticsAccessId = defaultUserStatisticsAccessId; }
	public void setDefaultUserSetupAccessId(String defaultUserSetupAccessId) { this.defaultUserSetupAccessId = defaultUserSetupAccessId; }

	/* Application Links */
	@Required public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
	@Required 	public void setExplorerUrl(String explorerUrl) { this.explorerUrlWithTrailingSlash = ensureTrailingSlash(NullSafe.nonNull(explorerUrl, "explorerUrlWithTrailingSlash")); }

	/* Relative servlet paths */
	public void setRelativeAboutPath(String aboutUrl) { this.relativeAboutPath = aboutUrl; }
	public void setRelativeDeploymentSummaryPath(String deploymentSummaryUrl) { this.relativeDeploymentSummaryPath = deploymentSummaryUrl; }
	public void setRelativeLogPath(String logUrl) { this.relativeLogPath = logUrl; }
	public void setRelativeSignInPath(String relativeSignInPath) { this.relativeSignInPath = relativeSignInPath; }
	public void setOnlineCompanyImageUrl(String onlineCompanyImageUrl) { this.onlineCompanyImageUrl = onlineCompanyImageUrl; }
	public void setCompanyUrl(String companyUrl) { this.onlineCompanyUrl = companyUrl; }
	public void setOnlineCompanyUrl(String onlineCompanyUrl) { this.onlineCompanyUrl = onlineCompanyUrl; }
	public void setCompanyImageUrl(String companyImageUrl) { this.onlineCompanyImageUrl = companyImageUrl; }
	public void setSystemRequestEvaluator(Evaluator<ServiceRequest> serviceRequestEvaluator) { this.systemServiceRequestEvaluator = serviceRequestEvaluator; }
	// @formatter:on

	public void addServiceDomainLinkConfigurer(BiConsumer<? super ServiceDomain, LinkCollection> configurer) {
		serviceDomainLinkConfigurers.add(configurer);
	}

	public void addAccessLinkConfigurer(BiConsumer<? super AccessDomain, LinkCollection> configurer) {
		accessLinkConfigurers.add(configurer);
	}

	@Override
	protected void serve(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		super.serve(req, resp);
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request, HttpServletResponse repsonse) {
		/* Check for current user-session */
		UserSession session = AttributeContexts.peek().findOrNull(UserSessionAspect.class);

		Home home = Home.T.create();
		if (isAccessGranted(session)) {

			// handle either tab or overview page.
			boolean wasTab = handleTab(request, home);
			if (!wasTab)
				handleOverview(home, session);
		}

		/* Prepare the template context. */
		VelocityContext context = new VelocityContext();

		/* Get packaging information */
		Packaging packaging = getPackaging();

		// /* Get Release Information */
		// Pair<List<VersionedArtifactIdentification>, Boolean> releaseArtifacts = getReleaseArtifacts();

		/* Build full Context */
		context.put("home", home);
		context.put("packaging", packaging);
		context.put("current_year", new GregorianCalendar().get(Calendar.YEAR));
		context.put("applicationName", applicationName);
		context.put("company_url", onlineCompanyUrl);
		context.put("company_image_url", onlineCompanyImageUrl);
		context.put("loginUrl", relativeSignInPath);

		// if (releaseArtifacts != null) {
		// context.put("releaseArtifacts", releaseArtifacts.first());
		// context.put("hasMoreReleaseArtifacts", releaseArtifacts.second());
		// }

		return context;
	}

	// private Pair<List<VersionedArtifactIdentification>, Boolean> getReleaseArtifacts() {
	// Pair<List<VersionedArtifactIdentification>, Boolean> versionedArtifacts = getReleaseArtifactsFromRepoViewResolution();
	//
	// if (versionedArtifacts != null)
	// return versionedArtifacts;
	//
	// return getReleaseArtifactsFromSetupAsset();
	// }
	//
	// private Pair<List<VersionedArtifactIdentification>, Boolean> getReleaseArtifactsFromSetupAsset() {
	// PlatformSetup platformSetup = platformSetupSupplier.get();
	// if (platformSetup == null) {
	// logger.info(() -> "Could not find the PlatformSetup.");
	// return null;
	// }
	//
	// PlatformAsset setupAsset = platformSetup.getSetupAsset();
	// return Pair.of(Collections.singletonList(VersionedArtifactIdentification.parse(setupAsset.qualifiedRevisionedAssetName())), false);
	// }
	//
	// private Pair<List<VersionedArtifactIdentification>, Boolean> getReleaseArtifactsFromRepoViewResolution() {
	// GetRepositoryViewResolution getRepoViewResolution = GetRepositoryViewResolution.T.create();
	// RepositoryViewResolution repositoryViewResolution = getRepoViewResolution.eval(systemServiceRequestEvaluator).get();
	//
	// if (repositoryViewResolution == null)
	// return null;
	//
	// Set<RepositoryViewSolution> visited = new HashSet<>();
	// List<RepositoryViewSolution> disjunctReleaseSolutions = new ArrayList<>();
	//
	// scanForDisjunctReleases(repositoryViewResolution.getTerminals(), visited, disjunctReleaseSolutions);
	//
	// List<String> artifacts = disjunctReleaseSolutions.stream() //
	// .map(RepositoryViewSolution::getArtifact) //
	// .collect(Collectors.toList()); //
	//
	// if (artifacts.isEmpty())
	// return null;
	//
	// Collections.reverse(artifacts);
	//
	// List<VersionedArtifactIdentification> versionedArtifacts = artifacts.stream() //
	// .limit(3) //
	// .map(VersionedArtifactIdentification::parse) //
	// .collect(Collectors.toList());
	//
	// boolean hasMore = artifacts.size() > versionedArtifacts.size();
	//
	// return Pair.of(versionedArtifacts, hasMore);
	// }
	//
	// private void scanForDisjunctReleases(List<RepositoryViewSolution> solutions, Set<RepositoryViewSolution> visited,
	// List<RepositoryViewSolution> disjunctReleaseSolutions) {
	//
	// for (RepositoryViewSolution solution : solutions) {
	// if (!visited.add(solution))
	// continue; // avoid visits of already visited assets
	//
	// if (solution.getRepositoryView().getRelease() != null) {
	// disjunctReleaseSolutions.add(solution);
	// continue;
	// }
	//
	// scanForDisjunctReleases(solution.getDependencies(), visited, disjunctReleaseSolutions);
	// }
	// }

	private void handleOverview(Home home, UserSession userSession) {
		handleGroup(userSession, home, true, () -> buildAdminGroup());
		// handleGroup(userSession, home, true, () -> buildModelGroup());
		handleGroup(userSession, home, false, () -> buildDomainGroup());
		handleGroup(userSession, home, false, () -> buildAccessGroup());
		// handleGroup(userSession, home, true, () -> buildFunctionalModuleGroup());
		// handleGroup(userSession, home, false, () -> buildWebTerminalGroup());
	}

	private void handleGroup(UserSession userSession, Home home, boolean isAdminRequired, Supplier<LinkGroup> groupSupplier) {
		if (!isAdminRequired || isAdminAccessGranted(userSession)) {
			LinkGroup linkGroup = groupSupplier.get();
			home.getGroups().add(linkGroup);
		}
	}

	// #################################################
	// ## . . . . . . . . AdminGroup . . . . . . . . .##
	// #################################################

	private LinkGroup buildAdminGroup() {
		LinkGroup adminGroup = LinkGroup.T.create();
		adminGroup.setName("Administration");
		adminGroup.setIconRef("./webpages/images/cortex/settings.png");

		fillAdminGroup(adminGroup);
		return adminGroup;
	}

	private void fillAdminGroup(LinkGroup administrationGroup) {
		LinkCollection cortexgroup = LinkCollection.T.create();
		cortexgroup.setDisplayName("Cortex");
		cortexgroup.setUrl("controlCenterUrl" + "#default");
		cortexgroup.setTarget("tfControlCenter-cortex");
		cortexgroup.setIconRef("./webpages/images/cortex/tf-cortex.png");

		cortexgroup.getNestedLinks().add(createLink("Administration", explorerUrlWithTrailingSlash + "#default", "tfControlCenter-cortex", null));

		configureAccessDomainLink(cortexgroup, "cortex");

		administrationGroup.getLinks().add(cortexgroup);

		LinkCollection usergroups = LinkCollection.T.create();
		usergroups.setDisplayName("User & Groups");
		usergroups.setUrl("controlCenterUrl" + "?accessId=" + defaultAuthAccessId + "#default");
		usergroups.setTarget("tfControlCenter-auth");
		usergroups.setIconRef("./webpages/images/cortex/groups.png");

		administrationGroup.getLinks().add(usergroups);

		LinkCollection runtimeStatus = LinkCollection.T.create();
		runtimeStatus.setDisplayName("Runtime");
		runtimeStatus.setIconRef("./webpages/images/cortex/runtime.png");

		String masterStatus = getPlatformVitalityStatus();

		runtimeStatus.getNestedLinks().add(createLink("About", "./home?selectedTab=ABOUT&selectedTabPath=" + relativeAboutPath, "_self", null));
		runtimeStatus.getNestedLinks().add(createLink("Health" + masterStatus,
				"./home?selectedTab=HEALTH&selectedTabPath=" + urlEncode(relativePlatformBaseChecksPath), "_self", null));
		runtimeStatus.getNestedLinks()
				.add(createLink("Checks", "./home?selectedTab=HEALTH&selectedTabPath=" + urlEncode(relativePlatformChecksPath), "_self", null));
		runtimeStatus.getNestedLinks().add(
				createLink("Log", "./home?selectedTab=LOGS&selectedTabPath=" + relativeLogPath, "_self", null, "./webpages/images/cortex/logs.png"));
		runtimeStatus.getNestedLinks()
				.add(createLink("Deployment Summary", "./home?selectedTab=DEPLOYMENT SUMMARY&selectedTabPath=" + relativeDeploymentSummaryPath,
						"_self", null, "./webpages/images/cortex/deploy.png"));
		administrationGroup.getLinks().add(runtimeStatus);
		// administrationGroup.getLinks().add(createLink("Logfiles",
		// "./home?selectedTab=LOGS&selectedTabPath="+relativeLogPath, "_self", null,
		// "./webpages/images/cortex/logs.png"));

		LinkCollection setupgroup = LinkCollection.T.create();
		setupgroup.setDisplayName("Platform Setup (Assets)");
		setupgroup.setIconRef("./webpages/images/cortex/asset.png");

		setupgroup.getNestedLinks().add(createLink("Administration", explorerUrlWithTrailingSlash + "?accessId=" + defaultUserSetupAccessId,
				"tfControlCenter-setup", null, "./webpages/images/cortex/asset.png"));

		configureAccessDomainLink(setupgroup, "setup");

		administrationGroup.getLinks().add(setupgroup);

		usergroups.getNestedLinks().add(createLink("Administration",
				explorerUrlWithTrailingSlash + "?accessId=" + urlEncode(defaultAuthAccessId) + "#default", "tfControlCenter-auth", null));

		if (accessExists(defaultUserSessionAccessId))
			usergroups.getNestedLinks()
					.add(createLink("User Sessions", explorerUrlWithTrailingSlash + "?accessId=" + urlEncode(defaultUserSessionAccessId) + "#default",
							"tfControlCenter-sessions", null));

		if (accessExists(defaultUserStatisticsAccessId))
			usergroups.getNestedLinks()
					.add(createLink("User Statistics",
							explorerUrlWithTrailingSlash + "?accessId=" + urlEncode(defaultUserStatisticsAccessId) + "#default",
							"tfControlCenter-statistics", null));
	}

	private String getPlatformVitalityStatus() {
		// TODO platform vitality status
		return "&nbsp;&#x2714;";
		// RunCheckBundles run = RunCheckBundles.T.create();
		// run.setCoverage(Collections.singleton(CheckCoverage.vitality));
		// run.setIsPlatformRelevant(true);
		//
		// CheckBundlesResponse response = run.eval(systemServiceRequestEvaluator).get();
		//
		// CheckStatus status = response.getStatus();
		// switch (status) {
		// case ok:
		// return "&nbsp;&#x2714;";
		// case warn:
		// return "&nbsp;&#x26a0;";
		// case fail:
		// default:
		// return "&nbsp;&#x2716;";
		// }
	}

	private boolean accessExists(String acccessId) {
		return accessDomains.byId(acccessId) != null;
	}

	// #################################################
	// ## . . . . . . . . ModelGroup . . . . . . . . .##
	// #################################################

	@SuppressWarnings("unused")
	private LinkGroup buildModelGroup() {
		LinkGroup modelGroup = LinkGroup.T.create();
		modelGroup.setName("Models");
		modelGroup.setIconRef("./webpages/images/cortex/models.png");
		modelGroup.setOpenLink(createLink("Open", explorerUrlWithTrailingSlash + "#do=showFolder&par.folderName=CustomModels", "tfControlCenter",
				null, "./webpages/images/open.png"));

		fillModelGroup(modelGroup);
		return modelGroup;
	}

	private void fillModelGroup(@SuppressWarnings("unused") LinkGroup modelsGroup) {
		//
//				//@formatter:off
//				SelectQuery query = new SelectQueryBuilder().from(GmMetaModel.T, "m")
//					.leftJoin("m", GmMetaModel.types, "t")
//					.where()
//						.negation()
//							.disjunction()
//								.property("m", GmMetaModel.name).ilike("tribefire.cortex*")
//								.property("m", GmMetaModel.name).ilike("com.braintribe*")
//							.close()
//					.select("m", GmMetaModel.name)
//					.select("m", GmMetaModel.version)
//					.select().count("t")
//					.orderBy(OrderingDirection.ascending).property("m", GmMetaModel.name)
//					.done();
//				//@formatter:on
		//
		// queryAndConsume(context.cortexSession, query, (r) -> {
		//
		// ListRecord lr = (ListRecord) r;
		// List<Object> values = lr.getValues();
		// String modelName = (String) values.get(0);
		// String version = (String) values.get(1);
		// long typeCount = (Long) values.get(2);
		//
		// if (typeCount > 0) {
		// LinkCollection links = LinkCollection.T.create();
		// links.setIconRef("./webpages/images/cortex/models.png");
		// if (StringTools.isEmpty(version)) {
		// version = "unknownVersion";
		//
		// }
		//
		// String[] nameElements = modelName.split(":");
		// String shortName = nameElements[nameElements.length - 1];
		//
		// String displayName = shortName; // + " (" + version + ")";
		//
		// links.setDisplayName(displayName);
		//
		// // TODO Remove the Standalone Modeler completely
		// // if (modelerUrl != null) {
		// // links.getNestedLinks()
		// // .add(createLink("Browse",
		// // modelerUrl + "#readOnly&do=loadModel&par.modelName=" +
		// // urlEncode(m.getName()),
		// // "tf-modeler", null));
		// // }
		// if (controlCenterUrl != null) {
		// links.getNestedLinks()
		// .add(createLink("Explore",
		// controlCenterUrl + "#do=query&par.typeSignature="
		// + urlEncode(GmMetaModel.T.getTypeSignature()) + "&par.propertyName=name&par.propertyValue="
		// + urlEncode(modelName),
		// "tfControlCenter", null));
		// }
		// /* links.getNestedLinks().add(createLink("Browse", ".", "tfModeler", null)); links.getNestedLinks().add(createLink("Exchange Package",
		// * ".", "tfModeler", null)); links.getNestedLinks().add(createLink("JSON", ".", "tfModeler", null)); */
		// modelsGroup.getLinks().add(links);
		// }
		//
		// });
	}

	// #################################################
	// ## . . . . . . . . DomainGroup . . . . . . . . ##
	// #################################################

	private LinkGroup buildDomainGroup() {
		LinkGroup serviceDomainsGroup = LinkGroup.T.create();
		serviceDomainsGroup.setName("Service Domains");
		serviceDomainsGroup.setIconRef("./webpages/images/cortex/domains.png");
		// This points to a folder, which does not exist currently
		// serviceDomainsGroup.setOpenLink(createLink("Open", ensureTrailingSlash(controlCenterUrl) + "#do=showFolder&par.folderName=CustomAccesses",
		// "tfControlCenter", null, "./webpages/images/open.png"));

		fillDomainGroup(serviceDomainsGroup);
		return serviceDomainsGroup;
	}

	private void fillDomainGroup(LinkGroup serviceDomainsGroup) {
		for (ServiceDomain sd : serviceDomains.list()) {
			String domainId = sd.domainId();

			// accesses are in a separate group
			if (accessDomains.hasDomain(domainId))
				continue;

			LinkCollection links = LinkCollection.T.create();
			links.setIconRef("./webpages/images/cortex/domains.png");

			setDisplayName(domainId, links);
			configureServiceDomainLink(links, domainId);

			if (links.getHasErrors()) {
				links.setDisplayName(links.getDisplayName() + " &#x2757;");
				serviceDomainsGroup.getLinks().add(links);

			} else if (!links.getNestedLinks().isEmpty()) {
				serviceDomainsGroup.getLinks().add(links);
			}
		}
	}

	// #################################################
	// ## . . . . . . . . AccessGroup . . . . . . . . ##
	// #################################################

	private LinkGroup buildAccessGroup() {
		LinkGroup serviceDomainsGroup = LinkGroup.T.create();
		serviceDomainsGroup.setName("Access Domains");
		serviceDomainsGroup.setIconRef("./webpages/images/cortex/domains.png");
		// This points to a folder, which does not exist currently
		// serviceDomainsGroup.setOpenLink(createLink("Open", ensureTrailingSlash(controlCenterUrl) + "#do=showFolder&par.folderName=CustomAccesses",
		// "tfControlCenter", null, "./webpages/images/open.png"));

		fillAccessGroup(serviceDomainsGroup);
		return serviceDomainsGroup;
	}

	private void fillAccessGroup(LinkGroup accessesGroup) {
		for (String accessId : accessDomains.domainIds()) {
			AccessDomain sd = accessDomains.byId(accessId);

			LinkCollection links = LinkCollection.T.create();
			links.setIconRef("./webpages/images/cortex/domains.png");

			setDisplayName(sd.access().getAccessId(), links);
			addAccessLinks(sd, links);
			configureAccessDomainLink(links, accessId);

			if (links.getHasErrors()) {
				links.setDisplayName(links.getDisplayName() + " &#x2757;");
				accessesGroup.getLinks().add(links);

			} else if (!links.getNestedLinks().isEmpty()) {
				accessesGroup.getLinks().add(links);
			}
		}
	}

	// #################################################
	// ## . . . . . FunctionalModuleGroup . . . . . . ##
	// #################################################

	@SuppressWarnings("unused")
	private LinkGroup buildFunctionalModuleGroup() {
		LinkGroup modulesGroup = LinkGroup.T.create();
		modulesGroup.setName("Functional Modules");
		modulesGroup.setIconRef("./webpages/images/cortex/modules.png");
		modulesGroup.setOpenLink(createLink("Open", explorerUrlWithTrailingSlash + "#do=showFolder&par.folderName=ShowFunctionalModules",
				"tfControlCenter", null, "./webpages/images/open.png"));

		fillFunctionalModulesGroup(modulesGroup);
		return modulesGroup;
	}

	private void fillFunctionalModulesGroup(@SuppressWarnings("unused") LinkGroup modulesGroup) {
//		//@formatter:off
//		SelectQuery query = new SelectQueryBuilder().from(Module.T, "m")
//			.where()
//				.disjunction()
//					.property("m", Module.bindsHardwired).eq(true)
//					.property("m", Module.bindsDeployables).eq(true)
//				.close()
//			.select("m", Module.globalId)
//			.select("m", Module.name)
//			.orderBy(OrderingDirection.ascending).property("m", Module.globalId)
//			.done();
//		//@formatter:on
		//
		// Map<String, String> vitalityCheckPerModule = getDistributedVitalityStatusByModule();
		// queryAndConsume(context.cortexSession, query, (m) -> {
		// ListRecord lr = (ListRecord) m;
		// List<Object> values = lr.getValues();
		// String globalId = (String) values.get(0);
		// String name = (String) values.get(1);
		//
		// LinkCollection links = LinkCollection.T.create();
		// links.setIconRef("./webpages/images/cortex/modules.png");
		//
		// links.setDisplayName(name == null ? "Unknown Module" : name);
		//
		// // links.getNestedLinks().add(createLink("About",
		// // "./home?selectedTab=CARTRIDGE&selectedTabPath="+urlEncode("cartridge/"+urlEncode(c.getExternalId())+"/aObout"),
		// // "_self", null));
		// String vitalityStatus = vitalityCheckPerModule.computeIfAbsent(globalId, s -> " &#x2714;");
		// links.getNestedLinks().add(createLink("Health" + vitalityStatus,
		// "./home?selectedTab=MODULE&selectedTabPath=" + urlEncode(relativeVitalityCheckPath + urlEncode(globalId)), "_self", null));
		//
		// links.getNestedLinks()
		// .add(createLink("Checks",
		// "./home?selectedTab=FUNCTIONAL MODULE&selectedTabPath=" + urlEncode(relativeModuleCheckPath + urlEncode(globalId)),
		// "_self", null));
		//
		// modulesGroup.getLinks().add(links);
		//
		// });
	}

	// private Map<String, String> getDistributedVitalityStatusByModule() {
	// Map<String, String> res = new HashMap<>();
	//
	// RunDistributedCheckBundles run = RunDistributedCheckBundles.T.create();
	// run.setCoverage(Collections.singleton(CheckCoverage.vitality));
	// run.setIsPlatformRelevant(false);
	// run.setAggregateBy(Collections.singletonList(CbrAggregationKind.module));
	//
	// CheckBundlesResponse response = run.eval(systemServiceRequestEvaluator).get();
	//
	// List<CbrAggregatable> elements = response.getElements();
	//
	// if (elements.isEmpty())
	// return res;
	//
	// for (CbrAggregatable a : response.getElements()) {
	// if (a instanceof CbrAggregation) {
	// CbrAggregation aggregation = (CbrAggregation) a;
	// Module module = (Module) aggregation.getDiscriminator();
	//
	// CheckStatus status = aggregation.getStatus();
	// String statusRepresentation = getStatusRepresentationAsStr(status);
	//
	// res.put(module.getGlobalId(), statusRepresentation);
	// }
	// }
	//
	// return res;
	// }
	//
	// private String getStatusRepresentationAsStr(CheckStatus status) {
	// String statusRepresentation = null;
	// switch (status) {
	// case ok:
	// statusRepresentation = " &#x2714;";
	// break;
	// case warn:
	// statusRepresentation = " &#x26a0;";
	// break;
	// case fail:
	// default:
	// statusRepresentation = " &#x2716;";
	// }
	// return statusRepresentation;
	// }

	// #################################################
	// ## . . . . . . . WebTerminalGroup . . . . . . .##
	// #################################################

	@SuppressWarnings("unused")
	private LinkGroup buildWebTerminalGroup() {
		LinkGroup webTerminalsGroup = LinkGroup.T.create();
		webTerminalsGroup.setName("Web Terminals");
		webTerminalsGroup.setIconRef("./webpages/images/cortex/webterminal.png");
		webTerminalsGroup.setOpenLink(createLink("Open", explorerUrlWithTrailingSlash + "#do=showFolder&par.folderName=Apps", "tfControlCenter", null,
				"./webpages/images/open.png"));

		fillWebTerminalGroup(webTerminalsGroup);
		return webTerminalsGroup;
	}

	private void fillWebTerminalGroup(@SuppressWarnings("unused") LinkGroup webTerminalsGroup) {
		// TODO find a way to list web terminals and check which are visible for current user

		// ModelAccessory modelAccessory = modelAccessoryFactory.getForModel(cortexAccess.getMetaModel().getName());
		// CmdResolver resolver = modelAccessory.getCmdResolver();
		//
		// if (resolver.getMetaData().entity(w).is(Visible.T)) {
		// webTerminalsGroup.getLinks().add(createLink(w.getName(), relativeWebTerminalPath + "/" + w.getPathIdentifier(),
		// "tfTerminal-" + w.getExternalId(), null, "./webpages/images/cortex/webterminal.png"));
		// }
	}

	// #################################################
	// ## . . . . . . . . . . Misc . . . . . . . . . .##
	// #################################################

	/** Configures both CRUD and API links, if available */
	private void configureAccessDomainLink(LinkCollection linkCollection, String accessId) {
		AccessDomain ad = accessDomains.byId(accessId);
		if (ad != null && isModelVisible(ad.configuredDataModel()))
			accessLinkConfigurers.forEach(c -> c.accept(ad, linkCollection));

		configureServiceDomainLink(linkCollection, accessId);
	}

	private void configureServiceDomainLink(LinkCollection linkCollection, String domainId) {
		ServiceDomain sd = serviceDomains.byId(domainId);
		if (sd != null && isModelVisible(sd.configuredModel()))
			serviceDomainLinkConfigurers.forEach(c -> c.accept(sd, linkCollection));
	}

	private boolean isModelVisible(ConfiguredModel configuredModel) {
		ModelMdResolver mdResolver = configuredModel.contextCmdResolver().getMetaData();
		mdResolver.useCases(USECASE_GME_LOGON);

		return mdResolver.is(Visible.T);
	}

	private static String urlEncode(String text) {
		try {
			return URLEncoder.encode(text, "UTF-8");
		} catch (Exception e) {
			logger.warn("Could not URL encode text: " + text);
			return "Unknown";
		}
	}

	private void addAccessLinks(AccessDomain ad, LinkCollection links) {
		try {
			if (isModelVisible(ad.configuredDataModel())) {
				String accessId = ad.access().getAccessId();
				links.getNestedLinks().add(//
						createLink("Explore", explorerUrlWithTrailingSlash + "?accessId=" + urlEncode(accessId) + "#default",
								"tfExplorer-" + accessId, null));

				// IncrementalAccess wbAccess = access.getWorkbenchAccess();
				// if (wbAccess != null) {
				// links.getNestedLinks()
				// .add(createLink("Work&shy;bench",
				// explorerUrlWithTrailingSlash + "?accessId=" + urlEncode(wbAccess.getExternalId()) + "#default",
				// "tfExplorer-" + wbAccess.getExternalId(), null));
				// }
			}
		} catch (Exception e) {
			logger.warn(() -> "Error while trying to get the links for access " + ad, e);
			links.setHasErrors(true);
		}
	}

	private void setDisplayName(String technicalName, LinkCollection links) {
		String name = technicalName;
		links.setDisplayName(name);
	}

	// private List<String> getListOfWorkbenchAccessIds(PersistenceGmSession cortexSession) {
//		//@formatter:off
//		SelectQuery query = new SelectQueryBuilder().from(IncrementalAccess.T, "a")
//				.join("a", IncrementalAccess.workbenchAccess, "w")
//				.select("w", IncrementalAccess.externalId).distinct(true)
//				.done();
//		//@formatter:off
//		List<String> list = cortexSession.query().select(query).list();
//		return list;
//	}
	
	
	private boolean handleTab(HttpServletRequest request, Home home) {
		String selectedTabParameter = request.getParameter("selectedTab");
		String selectedTabPathParameter = request.getParameter("selectedTabPath");
		if (selectedTabParameter != null && selectedTabPathParameter != null) {
			home.setSelectedTab(createLink(selectedTabParameter, selectedTabPathParameter, null, null));
			return true;
		}
		return false;
	}

	private boolean isAdminAccessGranted(UserSession session) {
		return isAccessGranted(session) && CollectionTools.containsAny(this.grantedRoles, session.getEffectiveRoles());
	}

	private boolean isAccessGranted(UserSession session) {
		return session != null;
	}

	private static Link createLink(String displayName, String url, String target, String type) {
		return createLink(displayName, url, target, type, null);
	}

	private static Link createLink(String displayName, String url, String target, String type, String iconRef) {
		if (url == null || url.isEmpty())
			return null;

		Link repositoryLink = Link.T.create();
		repositoryLink.setDisplayName(displayName);
		repositoryLink.setUrl(url);
		repositoryLink.setTarget(target);
		repositoryLink.setType(type);
		repositoryLink.setIconRef(iconRef);
		return repositoryLink;
	}

	private Packaging getPackaging() {
		try {
			Packaging packaging = packagingProvider.get();
			return packaging;
		} catch (Exception e) {
			logger.error("Could not read packaging information.", e);
			return null;
		}

	}

	private String ensureTrailingSlash(String path) {
		if (path == null)
			return "/";

		return path.endsWith("/") ? path : path + "/";
	}

}
