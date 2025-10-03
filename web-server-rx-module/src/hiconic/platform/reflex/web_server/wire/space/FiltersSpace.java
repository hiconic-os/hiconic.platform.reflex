package hiconic.platform.reflex.web_server.wire.space;

import com.braintribe.exception.LogPreferences;
import com.braintribe.logging.Logger.LogLevel;
import com.braintribe.utils.lcd.CollectionTools2;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.platform.reflex.web_server.processing.CallerInfoFilter;
import hiconic.platform.reflex.web_server.processing.CaptureFilter;
import hiconic.platform.reflex.web_server.processing.ThreadRenamerFilter;
import hiconic.platform.reflex.web_server.processing.cors.CorsFilter;
import hiconic.platform.reflex.web_server.processing.cors.handler.BasicCorsHandler;
import hiconic.platform.reflex.web_server.processing.exception.ExceptionFilter;
import hiconic.platform.reflex.web_server.processing.exception.StandardExceptionHandler;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.web.server.model.config.WebServerConfiguration;
import jakarta.servlet.Filter;

/**
 * @author peter.gazdik
 */
@Managed
public class FiltersSpace implements WireSpace {

	@Import
	private RxPlatformContract platform;

	@Import
	private RxPlatformResourcesContract platformResources;
	
	@Import
	private WebServerRxModuleSpace webServer;

	@Managed
	public CorsFilter corsFilter() {
		CorsFilter bean = new CorsFilter();
		bean.setCorsHandler(corsHandler());
		return bean;
	}

	@Managed
	private BasicCorsHandler corsHandler() {
		BasicCorsHandler bean = new BasicCorsHandler();
		bean.setConfiguration(configuration().getCorsConfiguration());
		return bean;
	}

	@Managed
	public CallerInfoFilter callerInfoFilter() {
		return new CallerInfoFilter();
	}

	@Managed
	public ThreadRenamerFilter threadRenamerFilter() {
		ThreadRenamerFilter bean = new ThreadRenamerFilter();
		bean.setThreadRenamer(platform.threadRenamer());
		return bean;
	}

	public CaptureFilter captureFilter() {
		CaptureFilter bean = new CaptureFilter();
		bean.setCaptureDir(platformResources.tmp("servlet-response-captures").asFile());
		return bean;
	}

	@Managed
	public Filter exceptionFilter() {
		ExceptionFilter bean = new ExceptionFilter();
		bean.setExceptionHandlers(CollectionTools2.asSet(standardExceptionHandler()));
		return bean;
	}

	@Managed
	private StandardExceptionHandler standardExceptionHandler() {
		WebServerConfiguration webConfig = configuration();

		StandardExceptionHandler bean = new StandardExceptionHandler();
		bean.setExceptionExposure(webConfig.getExceptionExposure());
		bean.setTracebackIdExposure(webConfig.getExposeTracebackId());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setRemoteAddressResolver(webServer.remoteAddressResolver());

		return bean;
	}

	@Managed
	private LogPreferences infoLogPreferences() {
		LogPreferences bean = new LogPreferences(LogLevel.INFO, false, LogLevel.TRACE);
		return bean;
	}

	private WebServerConfiguration configuration() {
		return platform.readConfig(WebServerConfiguration.T).get();
	}

}
