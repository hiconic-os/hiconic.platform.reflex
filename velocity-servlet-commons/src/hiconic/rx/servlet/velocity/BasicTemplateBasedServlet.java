// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.servlet.velocity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.StringTools;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class BasicTemplateBasedServlet extends HttpServlet {

	private static Logger logger = Logger.getLogger(BasicTemplateBasedServlet.class);

	public static final String STRING_TEMPLATE_PREFIX = "string-template:";

	private static final long serialVersionUID = -8502240092686297217L;
	public static final String DEFAULT_TEMPLATE_KEY = "_defaultTemplate";

	protected Map<String, Template> templateMap = new HashMap<>();
	protected Map<String, String> templateLocationMap = new HashMap<>();
	protected Map<String, String> templateStringMap = new HashMap<>();

	protected Map<Template, TemplateInfo> templateInfos = new HashMap<>();
	protected Map<String, Supplier<?>> contextProviders;
	protected VelocityTools tools = new VelocityTools();
	protected VelocityEngine velocityEngine = null;

	protected String contentType = "text/html";

	protected boolean refreshFileBasedTemplates = false;
	protected boolean templatesModified = false;

	@Configurable
	public void setRefreshFileBasedTemplates(boolean watchFileResources) {
		this.refreshFileBasedTemplates = watchFileResources;
	}

	@Configurable
	public void setTemplate(Template template) {
		this.templateMap.put(DEFAULT_TEMPLATE_KEY, template);
		templatesModified = true;
	}
	@Configurable
	public void setRelativeTemplateLocation(String relativeTemplateLocation) {
		setTemplateLocation(resolveRelativeTemplateLocation(relativeTemplateLocation));
	}
	@Configurable
	public void setTemplateLocation(String templateLocation) {
		this.templateLocationMap.put(DEFAULT_TEMPLATE_KEY, templateLocation);
		templatesModified = true;
	}

	@Configurable
	public void setContextProviders(Map<String, Supplier<?>> contextProviders) {
		this.contextProviders = contextProviders;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		service(req, resp, getClass().getClassLoader());
	}

	protected void service(HttpServletRequest req, HttpServletResponse resp, ClassLoader contextClassLoader) throws IOException, ServletException {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
			serve(req, resp);
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	protected void serve(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		resp.setContentType(this.getContentType());

		loadTemplates();

		Optional<UserSession> userSessionOptional = AttributeContexts.peek().findAttribute(UserSessionAspect.class);

		VelocityContext context = createContext(req, resp);
		addToContextIfNotExists(context, "tools", tools);
		addToContextIfNotExists(context, "request", req);
		addToContextIfNotExists(context, "userSession", userSessionOptional.orElse(null));
		addToContextIfNotExists(context, "sessionUser", userSessionOptional.map(UserSession::getUser).orElse(null));
		addToContextIfNotExists(context, "userIconSrc", "userIconSrc-not-supported"); // Deprecated. Use /user-image endpoint instead.

		if (contextProviders != null) {
			for (Map.Entry<String, Supplier<?>> contextEntry : contextProviders.entrySet()) {
				try {
					addToContextIfNotExists(context, contextEntry.getKey(), contextEntry.getValue().get());
				} catch (RuntimeException e) {
					logger.error("Error while adding context entry for key: " + contextEntry.getKey(), e);
				}
			}
		}

		Template template = resolveTemplate(context);

		PrintWriter writer = resp.getWriter();
		templateMerge(template, context, writer);
		writer.flush();
	}

	private Template resolveTemplate(VelocityContext context) throws IOException, ServletException {
		Template template = null;
		if (context instanceof TypedVelocityContext tvc) {
			String type = tvc.getType();
			if (type == null) {
				logger.debug("The typed velocity context does not contain a type. Using the default.");
				template = getTemplate(DEFAULT_TEMPLATE_KEY);
			} else {
				template = getTemplate(type);
			}

		} else {
			logger.trace(() -> "Using default template: " + this.templateLocationMap.get(DEFAULT_TEMPLATE_KEY));
			template = this.templateMap.get(DEFAULT_TEMPLATE_KEY);
		}

		if (template == null)
			throw new ServletException("No template found for context: " + context);

		return template;
	}

	protected Template getTemplate(String key) throws IOException {
		Template template = templateMap.get(key);

		if (refreshFileBasedTemplates) {
			TemplateInfo templateInfo = templateInfos.get(template);

			if (templateInfo != null && templateInfo.sourceFile != null) {
				Date lastModified = new Date(templateInfo.sourceFile.lastModified());

				if (!lastModified.equals(templateInfo.lastModified)) {
					velocityEngine = null;
					loadTemplates();
					return templateMap.get(key);
				}
			}
		}

		return template;
	}

	protected String getContentType() {
		return this.contentType;
	}
	@Configurable
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	protected void addToContextIfNotExists(VelocityContext context, String key, Object value) {
		if (context.get(key) == null) {
			context.put(key, value);
		}
	}

	/**
	 * Abstract method that should be implemented by subclasses. This is where all the data should be collected in a
	 * VelocityContext object. It is possible to return a {@link TypedVelocityContext} object that may contain information
	 * on which template should be used for rendering the response.
	 * 
	 * @param request
	 *            The HTTP servlet request.
	 * @param response
	 *            The HTTP servlet response.
	 * @return A VelocityContext object containing all the data that will go into the template.
	 */
	protected abstract VelocityContext createContext(HttpServletRequest request, HttpServletResponse response);

	protected void templateMerge(Template templateToMerge, Context context, Writer writer) throws IOException {
		try {
			templateToMerge.merge(context, writer);
		} catch (Exception e) {
			throw new IOException("failed to write to template: " + e.getMessage(), e);
		}
	}

	private static Object loadTemplateLock = new Object();

	protected void loadTemplates() throws IOException {
		if (this.velocityEngine == null || templatesModified) {
			synchronized (loadTemplateLock) {
				if (this.velocityEngine == null || templatesModified) {
					try {
						this.velocityEngine = com.braintribe.utils.velocity.VelocityTools.newResourceLoaderVelocityEngine(true);
					} catch (Exception e) {
						throw new IOException("Failed to initialize velocity engine: " + e.getMessage(), e);
					}

					for (Map.Entry<String, String> locationSet : this.templateLocationMap.entrySet()) {
						String key = locationSet.getKey();
						String location = locationSet.getValue();

						logger.debug(() -> "Loading template " + key + " at " + location);
						Template template = loadTemplate(this.velocityEngine, location);
						this.templateMap.put(key, template);

						ensureRefreshing(template, location);
					}

					if (templateStringMap != null && !templateStringMap.isEmpty()) {
						StringResourceRepository repo = StringResourceLoader.getRepository();

						for (Map.Entry<String, String> templateSet : this.templateStringMap.entrySet()) {
							String key = templateSet.getKey();
							String templateString = templateSet.getValue();

							logger.debug(
									() -> "Loading direct String template " + key + " with " + StringTools.getFirstNCharacters(templateString, 100));
							repo.putStringResource(key, templateString);

							Template template = velocityEngine.getTemplate(key);
							this.templateMap.put(key, template);
						}
					}
					templatesModified = false;
				}
			}
		}
	}

	private void ensureRefreshing(Template template, String location) {

		if (!refreshFileBasedTemplates)
			return;

		ClassLoader classLoader = getClass().getClassLoader();

		URL url = classLoader.getResource(location);

		if ("file".equalsIgnoreCase(url.getProtocol())) {
			try {
				File fileToRefresh = new File(url.toURI());

				TemplateInfo info = templateInfos.computeIfAbsent(template, t -> new TemplateInfo());
				info.sourceFile = fileToRefresh;
				info.lastModified = new Date(fileToRefresh.lastModified());
			} catch (URISyntaxException e) {
				logger.error("Error while trying to convert URL to File for monitoring template changes", e);
			}
		}
	}

	private static class TemplateInfo {
		public File sourceFile;
		public Date lastModified;
	}

	protected Template loadTemplate(VelocityEngine ve, String location) throws IOException {

		Template localTemplate = null;
		try {
			localTemplate = ve.getTemplate(location);
		} catch (Exception e) {
			throw new IOException("failed to load template from [ " + location + " ]: " + e.getMessage(), e);
		}

		if (localTemplate == null)
			throw new IOException("null template loaded from [ " + location + " ] ");

		return localTemplate;
	}

	public void addRelativeTemplateLocation(String key, String relativeTemplateLocation) throws IllegalArgumentException {
		addTemplateLocation(key, resolveRelativeTemplateLocation(relativeTemplateLocation));
	}

	/**
	 * This method can be used to programmatically add templates. Each template is to be identified by a key. Subclasses can
	 * return a {@link TypedVelocityContext} in the {@link #createContext(HttpServletRequest, HttpServletResponse)} method
	 * to influence which template to be used.
	 * 
	 * @param key
	 *            The unique key of the template, or "_defaultTemplate", if the default template should be changed.
	 * @param location
	 *            The template location, usually a file path or a resource path.
	 * @throws IllegalArgumentException
	 *             Thrown if any of the parameters is null.
	 */
	public void addTemplateLocation(String key, String location) throws IllegalArgumentException {
		if (key == null || location == null) {
			throw new IllegalArgumentException("Either the key " + key + " or the location " + location + " is null. This is not allowed.");
		}
		this.templateLocationMap.put(key, location);
		templatesModified = true;
	}

	/**
	 * Resolve given path relative to the current class's package.
	 */
	private String resolveRelativeTemplateLocation(String relativeTemplateLocation) {
		return getClass().getPackageName().replace('.', '/') + "/" + relativeTemplateLocation;
	}

	@Configurable
	public void setTemplateMap(Map<String, Template> templateMap) {
		this.templateMap = templateMap;
		templatesModified = true;
	}
	@Configurable
	public void setTemplateLocationMap(Map<String, String> templateLocationMap) {
		this.templateLocationMap = templateLocationMap;
		templatesModified = true;
	}
	@Configurable
	public void setTemplateStringMap(Map<String, String> templateStringMap) {
		this.templateStringMap = templateStringMap;
		templatesModified = true;
	}

}
