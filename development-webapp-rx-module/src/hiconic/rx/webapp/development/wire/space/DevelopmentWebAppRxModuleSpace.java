package hiconic.rx.webapp.development.wire.space;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.braintribe.logging.Logger;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.ModuleReflectionContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.webapp.development.processing.JinniArtifactDownloader;
import hiconic.rx.webapp.development.processing.MaterializedWebApp;
import hiconic.rx.webapp.development.processing.WebAppContribution;
import hiconic.rx.webapp.development.processing.WebAppContributionReader;
import hiconic.rx.webapp.development.processing.WebAppMaterializer;

@Managed
public class DevelopmentWebAppRxModuleSpace implements RxModuleContract {
	private static final Logger logger = Logger.getLogger(DevelopmentWebAppRxModuleSpace.class);
	private static final String DISABLED = "${RX_DEVELOPMENT_WEBAPPS_DISABLED:-false}";
	private static final String REFRESH = "${RX_DEVELOPMENT_WEBAPPS_REFRESH:-false}";

	@Import
	private RxPlatformContract platform;

	@Import
	private WebServerContract webServer;

	@Import
	private ModuleReflectionContract moduleReflection;

	@Override
	public void onDeploy() {
		if (Boolean.parseBoolean(resolve(DISABLED))) {
			logger.info("Development web application materialization is disabled.");
			return;
		}

		Path applicationRoot = platform.applicationFiles().rootPath().toAbsolutePath().normalize();
		if (Files.isRegularFile(applicationRoot.resolve("packaged-solutions.txt"))) {
			logger.debug("Packaged application detected; development web application materialization is not needed.");
			return;
		}

		List<WebAppContribution> contributions = contributionReader().read(moduleReflection.moduleClassLoader());
		if (contributions.isEmpty()) {
			logger.debug("No development web application contributions found on the active classpath.");
			return;
		}

		Path cache = applicationRoot.resolve("build/development-runtime/web-apps");
		boolean refresh = Boolean.parseBoolean(resolve(REFRESH));
		List<MaterializedWebApp> webApps = materializer(cache).materialize(contributions, refresh);
		for (MaterializedWebApp webApp : webApps) {
			WebAppContribution contribution = webApp.contribution();
			String[] welcomeFiles = contribution.welcomeFile() == null ? new String[0] : new String[] { contribution.welcomeFile() };
			webServer.addStaticFileResource(contribution.serverPath(), webApp.contentDirectory().toString(), welcomeFiles);
			logger.info("Serving development web application " + contribution.dependency() + " at " + contribution.serverPath());
		}
	}

	@Managed
	private WebAppContributionReader contributionReader() {
		return new WebAppContributionReader();
	}

	@Managed
	private WebAppMaterializer materializer(Path cache) {
		return new WebAppMaterializer(cache, new JinniArtifactDownloader());
	}

	private String resolve(String expression) {
		return platform.configuration().propertyResolver().resolve(expression);
	}
}
