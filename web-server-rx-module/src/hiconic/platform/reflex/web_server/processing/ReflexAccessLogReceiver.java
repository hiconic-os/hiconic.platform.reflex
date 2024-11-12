package hiconic.platform.reflex.web_server.processing;

import com.braintribe.logging.Logger;

public class ReflexAccessLogReceiver implements io.undertow.server.handlers.accesslog.AccessLogReceiver {
	private Logger logger = Logger.getLogger(ReflexAccessLogReceiver.class);
	@Override
	public void logMessage(String message) {
		//logger.info(message);
	}
}