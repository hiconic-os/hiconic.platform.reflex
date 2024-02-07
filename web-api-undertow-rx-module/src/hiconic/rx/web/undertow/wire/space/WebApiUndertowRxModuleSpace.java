package hiconic.rx.web.undertow.wire.space;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import javax.servlet.ServletException;

import org.jboss.logging.Logger;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.ddra.endpoints.api.api.v1.DdraMappings;
import com.braintribe.model.resource.api.MimeTypeRegistry;
import com.braintribe.model.resource.utils.MimeTypeRegistryImpl;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.servlet.ApiV1RestServletUtils;
import hiconic.rx.web.servlet.DdraEndpointsExceptionHandler;
import hiconic.rx.web.servlet.WebApiV1Server;
import hiconic.rx.web.undertow.model.config.UndertowConfiguration;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

@Managed
public class WebApiUndertowRxModuleSpace implements RxModuleContract {
	private static Logger logger = Logger.getLogger(WebApiUndertowRxModuleSpace.class);

	private static final String MIME_TYPE_JSON = "application/json";
	@Import
	private RxPlatformContract platform;
	
	@Import
	private TcSpace tc;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		undertowServer().start();
	}
	
	@Managed
	private Undertow undertowServer() {
		int port = platform.readConfig(UndertowConfiguration.T).get().getPort();
		
		WebApiV1Server servlet = server();
		
		ServletInfo servletInfo = Servlets.servlet("api", WebApiV1Server.class, new ImmediateInstanceFactory<>(servlet));
		servletInfo.addMapping("/api/*");
		
		DeploymentInfo deploymentInfo = Servlets.deployment() //
				.setClassLoader(Undertow.class.getClassLoader())
				.setContextPath("/")
				.setDeploymentName("api-deployment") //
				.addServlets(servletInfo);
		
		DeploymentManager manager = Servlets.defaultContainer() //
				.addDeployment(deploymentInfo);
		
	    manager.deploy();
	    
	    HttpHandler handler;
		try {
			handler = manager.start();
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
		
		//PathHandler path = Handlers.path(Handlers.redirect("/api")).addPrefixPath("/api", handler);
		PathHandler path = Handlers.path(handler);
	    
		Undertow bean = Undertow.builder()
				.addHttpListener(port, "localhost")
				.setHandler(path)				      
				.build();
		
		return bean;
	}
	
	@Managed
	private WebApiV1Server server() {
		WebApiV1Server bean = new WebApiV1Server();
		bean.setDefaultServiceDomain("main");
		bean.setEvaluator(platform.evaluator());
		bean.setExceptionHandler(exceptionHandler());
		bean.setMappings(new DdraMappings());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setMdResolverProvider(d -> platform.mdResolver());
		bean.setRestServletUtils(servletUtils());
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		bean.setTraversingCriteriaMap(tc.criteriaMap());
		return bean;
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
		try (InputStream in = getClass().getResource("mime-extensions.properties").openStream()) {
			return StringTools.readLinesFromInputStream(in, "UTF-8", false);

		} catch (IOException e) {
			throw new UncheckedIOException("Error while reading mime-extensions.properties", e);
		}
	}
	
	private MimeTypeRegistry configureMimeTypeRegistry(MimeTypeRegistryImpl bean) {
		// TODO 28.2.2023 This file also exists in platform-api under com/braintribe/mimetype/mime-extensions.properties
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
