// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.explorer.processing.servlet.explorer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.access.ModelAccessException;
import com.braintribe.model.folder.Folder;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.resource.Icon;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.workbench.WorkbenchConfiguration;

import hiconic.rx.explorer.model.configuration.ExplorerConfiguration;
import hiconic.rx.explorer.model.configuration.ModelEnvironmentConfiguration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Supplies the small dynamic resources expected by the Explorer bootstrap page.
 *
 * <p>This deliberately lives in the Explorer module. These resources are an
 * Explorer protocol and are not a generic web-server concern.</p>
 */
public class ExplorerPublicResourceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ExplorerPublicResourceServlet.class);
	private static final String DEFAULT_LOGO = "/HICONIC-PUBLIC-RESOURCES/explorer/webpages/hiconic-logo.svg";

	private PersistenceGmSessionFactory sessionFactory;
	private final Map<String, String> workbenchAccessIds = new HashMap<>();
	private String defaultDataAccessId;

	@Required
	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Required
	public void setConfiguration(ExplorerConfiguration configuration) {
		defaultDataAccessId = configuration.getDefaultDataAccessId();
		for (ModelEnvironmentConfiguration environment : configuration.getModelEnvironments())
			workbenchAccessIds.put(environment.getDataAccessId(), environment.getWorkbenchAccessId());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setHeader("Pragma", "no-cache");

		String resourceName = normalizePath(request.getPathInfo());
		String accessId = request.getParameter("accessId");
		try {
			switch (resourceName) {
				case "gme-title" -> writeText(response, title(accessId), "text/plain");
				case "gme-locale" -> writeText(response, locale(accessId), "text/plain");
				case "gme-logo" -> writeLogo(response, accessId);
				case "gme-css", "UiTheme" -> writeConfigurationResource(response, accessId, WorkbenchConfiguration::getStylesheet, "text/css");
				case "gme-favicon" -> writeConfigurationResource(response, accessId, WorkbenchConfiguration::getFavIcon, "image/x-icon");
				default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (RuntimeException e) {
			log.warn("Could not resolve Explorer public resource '" + resourceName + "' for access '" + accessId + "'", e);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private String title(String accessId) throws ModelAccessException {
		WorkbenchConfiguration configuration = configuration(accessId);
		return configuration != null && configuration.getTitle() != null ? configuration.getTitle() : "tribefire";
	}

	private String locale(String accessId) throws ModelAccessException {
		WorkbenchConfiguration configuration = configuration(accessId);
		String locale = configuration == null ? null : configuration.getLocale();
		return locale == null || locale.isBlank() || "auto".equalsIgnoreCase(locale) ? "en" : locale;
	}

	private WorkbenchConfiguration configuration(String accessId) throws ModelAccessException {
		PersistenceGmSession session = workbenchSession(accessId);
		if (session == null)
			return null;

		return session.query().entities(EntityQueryBuilder.from(WorkbenchConfiguration.T).tc().negation().joker().done()).first();
	}

	private void writeLogo(HttpServletResponse response, String accessId) throws ModelAccessException, IOException {
		PersistenceGmSession session = workbenchSession(accessId);
		if (session == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Folder folder = session.query().entities(EntityQueryBuilder.from(Folder.T).where().property(Folder.name).eq("tb_Logo") //
				.tc().negation().joker().done()).first();
		Icon icon = folder == null ? null : folder.getIcon();
		Resource logo = icon == null ? null : icon.image();
		if (logo == null) {
			writeDefaultLogo(response);
			return;
		}

		if (logo.getMimeType() != null)
			response.setContentType(logo.getMimeType());
		try (InputStream in = logo.openStream()) {
			in.transferTo(response.getOutputStream());
		}
	}

	private void writeConfigurationResource(HttpServletResponse response, String accessId,
			java.util.function.Function<WorkbenchConfiguration, Resource> resourceProvider, String defaultMimeType)
			throws ModelAccessException, IOException {
		WorkbenchConfiguration configuration = configuration(accessId);
		Resource resource = configuration == null ? null : resourceProvider.apply(configuration);
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		response.setContentType(resource.getMimeType() == null || resource.getMimeType().isBlank() ? defaultMimeType : resource.getMimeType());
		try (InputStream in = resource.openStream()) {
			in.transferTo(response.getOutputStream());
		}
	}

	private void writeDefaultLogo(HttpServletResponse response) throws IOException {
		try (InputStream in = ExplorerPublicResourceServlet.class.getResourceAsStream(DEFAULT_LOGO)) {
			if (in == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			response.setContentType("image/svg+xml");
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			in.transferTo(response.getOutputStream());
		}
	}

	private PersistenceGmSession workbenchSession(String accessId) {
		if (accessId == null || accessId.isBlank())
			accessId = defaultDataAccessId;
		String workbenchAccessId = workbenchAccessIds.get(accessId);
		return workbenchAccessId == null ? null : sessionFactory.newSession(workbenchAccessId);
	}

	private static void writeText(HttpServletResponse response, String value, String mimeType) throws IOException {
		response.setContentType(mimeType);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(value);
	}

	private static String normalizePath(String path) {
		if (path == null)
			return "";
		return path.startsWith("/") ? path.substring(1) : path;
	}
}
