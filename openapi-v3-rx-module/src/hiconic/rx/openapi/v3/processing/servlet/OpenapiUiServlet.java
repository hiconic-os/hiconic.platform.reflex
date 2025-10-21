// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.openapi.v3.processing.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.meta.data.prompt.Visible;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.servlet.velocity.BasicTemplateBasedServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Supported URLs:
 * <ul>
 * <li>/openapi/ui/services/my-domain?useCases=openapi:simple
 * </ul> 
 */
public class OpenapiUiServlet extends BasicTemplateBasedServlet {

	private static final long serialVersionUID = -3597570947500170486L;

	private static final Logger logger = Logger.getLogger(OpenapiUiServlet.class);

	private String servicesUrl;
	private Function<HttpServletRequest, String> swaggerfileUrlFactory = this::swaggerfileUrlFactory;

	private ServiceDomains serviceDomains;
	private AccessDomains accessDomains;

	private boolean wasInitialized;

	private String webApiPath = "api";
	private String restPath = "rest";

	// @formatter:off
	@Required public void setServiceDomains(ServiceDomains serviceDomains) { this.serviceDomains = serviceDomains; }
	@Required public void setAccessDomains(AccessDomains accessDomains) { this.accessDomains = accessDomains; }
	
	/** Configure the path of the WebApi servlet. Default is 'api' */
	@Configurable public void setWebApiPath(String webApiPath) { this.webApiPath = webApiPath; }
	@Configurable public void setRestPath(String restPath) { this.restPath = restPath; }
	// @formatter:on

	@Override
	public void init() {
		setRelativeTemplateLocation("openapi_ui.vm");
		setContentType("text/html;charset=UTF-8");
		wasInitialized = true;
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request, HttpServletResponse response) {
		// Uncomment the following lines during development:
		// templatesModified = true;
		// refreshFileBasedTemplates = true;

		VelocityContext context = new VelocityContext();
		ensureServicesUrl();

		String currentSwaggerfileUrl = servicesUrl + swaggerfileUrlFactory.apply(request);

		List<HeaderLink> headerLinks = createHeaderLinks(request);

		context.put("swaggerfileUrl", currentSwaggerfileUrl);
		context.put("swaggerUiBundleJsUrl", resolveRelativeTemplateLocation("swagger-ui-bundle.js"));
		context.put("swaggerUiStandalonePresetJsUrl", resolveRelativeTemplateLocation("swagger-ui-standalone-preset.js"));
		context.put("servicesUrl", servicesUrl); // needed for display resources like css
		context.put("headerLinks", headerLinks);

		return context;
	}

	private void ensureServicesUrl() {
		if (servicesUrl == null) {
			servicesUrl = "../../.."; // relative services path: e.g. services/openapi/ui/my-domain/services -> services
		}
	}

	private List<HeaderLink> createHeaderLinks(HttpServletRequest request) {
		String[] splitPathInfo = request.getPathInfo().split("/");
		int pathElementCount = splitPathInfo.length - 1;

		if (pathElementCount != 2) {
			throw new IllegalArgumentException(
					"The url path should end with exactly 2 elements describing mode and domain of the model that should be exported but found only "
							+ pathElementCount);
		}

		String delegate = splitPathInfo[1];
		String domainId = splitPathInfo[2];

		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		if (serviceDomain == null)
			throw new IllegalArgumentException("'" + domainId + "' is no valid access or service domain.");

		String[] useCases = request.getParameterValues("useCases");
		String queryParameter = useCases == null ? "" : "?useCases=" + String.join("&useCases=", useCases);

		boolean shouldReflectServices = delegate.equals("services");

		List<HeaderLink> headerLinks = new ArrayList<>();

		AccessDomain accessDomain = accessDomains.byId(domainId);
		if (accessDomain != null) {
			if (modelIsVisible(accessDomain.configuredDataModel(), useCases)) {
				headerLinks.add(headerLink("CRUD Entities", "entities", delegate, domainId, queryParameter));
				headerLinks.add(headerLink("CRUD Properties", "properties", delegate, domainId, queryParameter));
			} else if (!shouldReflectServices) {
				logger.debug("No permission to reflect meta model for access '" + serviceDomain.domainId() + "'.");
			}
		}

		if (modelIsVisible(serviceDomain.configuredModel(), useCases)) {
			headerLinks.add(headerLink("Service Requests", "services", delegate, domainId, queryParameter));
		} else if (shouldReflectServices) {
			logger.debug("No permission to reflect service model for domain '" + serviceDomain.domainId() + "'. ");
		}

		// TODO make hideSwagger20 configurable?
		String hideSwagger20 = "false";
		if (!"true".equalsIgnoreCase(hideSwagger20)) {
			if (shouldReflectServices) {
				headerLinks.add(new HeaderLink("Swagger 2.0", servicesUrl + "/"+webApiPath+"/" + domainId, false, true));
			} else {
				headerLinks.add(new HeaderLink("Swagger 2.0", servicesUrl + "/" + restPath + "/" + delegate + "/" + domainId, false, true));
			}
		}

		return headerLinks;
	}

	private static boolean modelIsVisible(ConfiguredModel configuredModel, String[] useCases) {
		ModelMdResolver modelMdResolver = configuredModel.contextCmdResolver().getMetaData();

		if (useCases != null)
			modelMdResolver.useCases(useCases);

		return modelMdResolver.useCase("openapi").is(Visible.T);
	}

	private HeaderLink headerLink(String title, String linkDelegate, String currentDelegate, String domain, String queryParameter) {
		return new HeaderLink(title, "../" + linkDelegate + "/" + domain + queryParameter, linkDelegate.equals(currentDelegate), false);
	}

	private void assertNotInitialized() {
		if (wasInitialized) {
			throw new IllegalStateException("Servlet was already initialized and can't be configured any more.");
		}
	}

	private String swaggerfileUrlFactory(HttpServletRequest request) {
		String apiV1 = "/" + webApiPath + "/openapi/";
		String[] splitPathInfo = request.getPathInfo().split("/");

		if (splitPathInfo.length != 3) {
			throw new IllegalArgumentException(
					"Openapi UI servlet expects two path elements: one for the request kind and one for the domainId to reflect but got: "
							+ request.getPathInfo());
		}

		// splitPathInfo[0] is empty because pathInfo always starts with a slash if its present
		String requestKind = splitPathInfo[1];
		String domainId = splitPathInfo[2];
		String domainProperty = "services".equals(requestKind) ? "serviceDomain" : "accessId";

		String baseRequest = apiV1 + requestKind + "?" + domainProperty + "=" + domainId;

		if (request.getQueryString() != null) {
			return baseRequest + "&" + request.getQueryString();
		}

		return baseRequest;
	}

	public static class HeaderLink {
		public String title;
		public String url;
		public boolean active;
		public boolean newWindow;

		public HeaderLink(String title, String url, boolean active, boolean newWindow) {
			super();
			this.title = title;
			this.url = url;
			this.active = active;
			this.newWindow = newWindow;
		}

		public String getTitle() {
			return title;
		}

		public String getUrl() {
			return url;
		}

		public boolean getActive() {
			return active;
		}

		public boolean isActive() {
			return active;
		}

		public String getTarget() {
			return newWindow ? "_blank" : "_self";
		}
	}

	@Configurable
	public void setServicesUrl(String servicesUrl) {
		assertNotInitialized();
		this.servicesUrl = servicesUrl;
	}

	@Configurable
	@Required
	public void setSwaggerfileUrlFactory(Function<HttpServletRequest, String> swaggerfileUrlFactory) {
		this.swaggerfileUrlFactory = swaggerfileUrlFactory;
	}
}
