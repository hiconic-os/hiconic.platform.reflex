// ============================================================================
package hiconic.rx.explorer.processing.servlet.home;

import java.net.URLEncoder;
import java.util.function.BiConsumer;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;
import com.braintribe.model.meta.data.prompt.Visible;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.explorer.home.model.Link;
import hiconic.rx.explorer.home.model.LinkCollection;
import hiconic.rx.module.api.service.ServiceDomain;

public class OpenApiLandingPageLinkConfigurer implements BiConsumer<Object, LinkCollection> {

	private static final Logger logger = Logger.getLogger(OpenApiLandingPageLinkConfigurer.class);
	
	private static final String USECASE_OPENAPI = "openapi";
	private static final String USECASE_SWAGGER = "swagger";
	private static final String USECASE_DDRA = "ddra";
	
	private final String simpleUseCase = "useCases=openapi:simple";
	private String relativeApiPath = "openapi/ui/services/{domainId}?" + simpleUseCase;
	private String relativeCrudPath = "openapi/ui/entities/{domainId}?" + simpleUseCase;
	
	/**
	 * @param relativeCrudPath
	 *            path to a UI representation of the CRUD API of an access - e.g. a Swagger UI. The path is relative to
	 *            tf-services and should contain the String <tt>{domainId}</tt> as a placeholder for the actual domain id (=
	 *            externalId) of the access like <tt>openapi/ui/entities/{domainId}</tt>
	 */
	@Configurable
	public void setRelativeCrudPath(String relativeCrudPath) {
		this.relativeCrudPath = relativeCrudPath;
	}

	/**
	 * @param relativeApiPath
	 *            path to a UI representation of the API of a service domain - e.g. a Swagger UI. The path is relative to
	 *            tf-services and should contain the String <tt>{domainId}</tt> as a placeholder for the actual domain id
	 *            like <tt>openapi/ui/services/{domainId}</tt>
	 */
	@Configurable
	public void setRelativeApiPath(String relativeApiPath) {
		this.relativeApiPath = relativeApiPath;
	}

	@Override
	public void accept(Object domain, LinkCollection linkCollection) {
		if (domain instanceof ServiceDomain sd )
			addServiceDomainLinks(sd, linkCollection);
		else if (domain instanceof AccessDomain ad)
			addAccessDomainLinks(ad, linkCollection);
	}

	private void addServiceDomainLinks(ServiceDomain domain, LinkCollection links) {
		String domainId = domain.domainId();

		try {
			if (isModelVisible(domain.contextCmdResolver(), USECASE_DDRA, USECASE_OPENAPI, USECASE_SWAGGER)) {
				addApiLink(domainId, links);
			} 
			
		} catch (Exception e) {
			logger.warn(() -> "Error while getting API links for domain " + domain, e);
			links.setHasErrors(true);
		}
	}

	private void addAccessDomainLinks(AccessDomain domain, LinkCollection links) {
		String domainId = domain.access().getAccessId();
		
		try {
			if (isModelVisible(domain.configuredDataModel().contextCmdResolver(), USECASE_DDRA, USECASE_OPENAPI, USECASE_SWAGGER)) {
				addCrudApiLink(links, domainId);
			}
		} catch (Exception e) {
			logger.warn(() -> "Error while getting API links for domain " + domain, e);
			links.setHasErrors(true);
		}
	}
	
	private boolean isModelVisible(CmdResolver cmdResolver, String... useCases) {
		ModelMdResolver mdResolver = cmdResolver.getMetaData();
		if (useCases != null && useCases.length > 0) {
			mdResolver.useCases(useCases);
		}
		return mdResolver.is(Visible.T);
	}
	
	private void addApiLink(String domainId, LinkCollection links) {
		String tabLink = tabLink("Service API", resolveApiPath(relativeApiPath, domainId));
		links.getNestedLinks().add(createLink("Service API", tabLink, "_self", null));
	}

	private void addCrudApiLink(LinkCollection links, String domainId) {
		String tabLink = tabLink("Access API", resolveApiPath(relativeCrudPath, domainId));
		links.getNestedLinks().add(createLink("Access API", tabLink, "_self", null));
	}

	private String resolveApiPath(String apiPath, String domainId) {
		return apiPath.replace("{domainId}", urlEncode(domainId));
	}
	
	private String tabLink(String title, String path) {
		return "./home?selectedTab=" + title + "&selectedTabPath=" + urlEncode(path);
	}
	
	private static String urlEncode(String text) {
		try {
			return URLEncoder.encode(text, "UTF-8");
		} catch (Exception e) {
			logger.warn("Could not URL encode text: " + text);
			return "Unknown";
		}
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

}
