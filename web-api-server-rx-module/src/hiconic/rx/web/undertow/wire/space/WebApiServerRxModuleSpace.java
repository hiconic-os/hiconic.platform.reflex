package hiconic.rx.web.undertow.wire.space;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.jboss.logging.Logger;

import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.resource.api.MimeTypeRegistry;
import com.braintribe.model.resource.utils.MimeTypeRegistryImpl;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import dev.hiconic.servlet.ddra.endpoints.api.api.v1.DdraMappings;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.web.servlet.ApiV1RestServletUtils;
import hiconic.rx.web.servlet.DdraEndpointsExceptionHandler;
import hiconic.rx.web.servlet.WebApiV1Server;

@Managed
public class WebApiServerRxModuleSpace implements RxModuleContract {
	private static Logger logger = Logger.getLogger(WebApiServerRxModuleSpace.class);

	private static final String MIME_TYPE_JSON = "application/json";
	@Import
	private RxPlatformContract platform;
	
	@Import
	private TcSpace tc;
	
	@Import
	private WebServerContract webServer;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		webServer.addServlet("web-api", "/api/*", server());
	}
	
	@Managed
	private WebApiV1Server server() {
		WebApiV1Server bean = new WebApiV1Server();
		bean.setDefaultServiceDomain("main");
		bean.setEvaluator(platform.evaluator());
		bean.setExceptionHandler(exceptionHandler());
		bean.setMappings(new DdraMappings());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setMdResolverProvider(this::cmdResolverForDomain);
		bean.setRestServletUtils(servletUtils());
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		bean.setTraversingCriteriaMap(tc.criteriaMap());
		return bean;
	}
	
	private CmdResolver cmdResolverForDomain(String domainId) {
		ServiceDomain domain = platform.serviceDomains().byId(domainId);
		
		if (domain == null)
			return null;
		
		return domain.contextCmdResolver();
	}
	
	private ApiV1RestServletUtils servletUtils() {
		ApiV1RestServletUtils bean = new ApiV1RestServletUtils();
		bean.setMimeTypeRegistry(mimeTypeRegistry());
		return bean;
	}
	
	private MimeTypeRegistryImpl mimeTypeRegistry() {
		MimeTypeRegistryImpl bean = new MimeTypeRegistryImpl();
		// TODO: currently deactivated to reduce bootstrap impedance. Think how this could be improved
		// configureMimeTypeRegistry(bean);
		return bean;
	}
	
	private List<String> readMimeExtensionsProperties() {
		// TODO 28.2.2023 This file also exists in platform-api under com/braintribe/mimetype/mime-extensions.properties
		try (InputStream in = getClass().getResource("mime-extensions.properties").openStream()) {
			return StringTools.readLinesFromInputStream(in, "UTF-8", false);

		} catch (IOException e) {
			throw new UncheckedIOException("Error while reading mime-extensions.properties", e);
		}
	}

	@SuppressWarnings("unused")
	private MimeTypeRegistry configureMimeTypeRegistry(MimeTypeRegistryImpl bean) {
		List<String> lines = readMimeExtensionsProperties();

		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("#")) {
				continue;
			}
			if (StringTools.isBlank(line)) {
				continue;
			}
			int idx = line.indexOf(":");
			if (idx <= 0) {
				logger.debug("Could not understand line: " + line + " of mime-extensions.properties");
				continue;
			}
			String mimeType = line.substring(0, idx).trim();
			if (StringTools.isBlank(mimeType)) {
				logger.debug("Could not get Mime-Type from line: " + line + " of mime-extensions.properties");
				continue;
			}
			if (!mimeType.contains("/")) {
				logger.debug("Could not get valid Mime-Type from line: " + line + " of mime-extensions.properties");
				continue;
			}
			String extensions = line.substring(idx + 1).trim();
			if (extensions.equals("[]")) {
				continue;
			}
			if (!extensions.startsWith("[") && !extensions.endsWith("]")) {
				logger.debug("Could not understand extensions " + extensions + " of line: " + line + " of mime-extensions.properties");
				continue;
			}
			extensions = StringTools.removeFirstAndLastCharacter(extensions);
			String[] extArray = StringTools.splitCommaSeparatedString(extensions, true);
			if (extArray.length == 0) {
				logger.debug("Could not understand extensions " + extensions + " of line: " + line + " of mime-extensions.properties");
				continue;
			}
			for (String ext : extArray) {
				bean.registerMapping(mimeType, ext);
			}
		}

		return bean;
	}
	
	@Managed
	private DdraEndpointsExceptionHandler exceptionHandler() {
		DdraEndpointsExceptionHandler bean = new DdraEndpointsExceptionHandler();
		
		bean.setDefaultMimeType(MIME_TYPE_JSON);
		bean.setDefaultMarshaller(platform.marshallers().getMarshaller(MIME_TYPE_JSON));
		bean.setIncludeDebugInformation(false);
		
		return bean;
	}
}
