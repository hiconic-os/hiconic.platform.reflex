package hiconic.platform.reflex.web_server.processing;

import hiconic.rx.module.api.state.RxApplicationStateManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ApplicationStateGateHandler implements HttpHandler {

	private static final String LIVENESS_PATH = "/livez";
	private static final String READINESS_PATH = "/readyz";

	private final BlockingHolder<HttpHandler> standardHandler = new BlockingHolder<>();
	private final RxApplicationStateManager stateManager;
	private final String livenessPathAlias;
	private final String readinessPathAlias;
	
	public ApplicationStateGateHandler(RxApplicationStateManager stateManager) {
		this(stateManager, null);
	}

	public ApplicationStateGateHandler(RxApplicationStateManager stateManager, String healthEndpointAliasBasePath) {
		super();
		this.stateManager = stateManager;
		this.livenessPathAlias = aliasPath(healthEndpointAliasBasePath, LIVENESS_PATH);
		this.readinessPathAlias = aliasPath(healthEndpointAliasBasePath, READINESS_PATH);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getRelativePath();
		
		if (LIVENESS_PATH.equals(path) || livenessPathAlias != null && livenessPathAlias.equals(path))
			handleLivez(exchange);
		else if (READINESS_PATH.equals(path) || readinessPathAlias != null && readinessPathAlias.equals(path))
			handleReadyz(exchange);
		else
			handleStandardRequest(exchange);
	}

	private static String aliasPath(String basePath, String canonicalPath) {
		if (basePath == null || basePath.isBlank() || "/".equals(basePath))
			return null;

		String normalizedBasePath = basePath.startsWith("/") ? basePath : "/" + basePath;
		while (normalizedBasePath.endsWith("/"))
			normalizedBasePath = normalizedBasePath.substring(0, normalizedBasePath.length() - 1);

		return normalizedBasePath + canonicalPath;
	}
	
	private void handleStandardRequest(HttpServerExchange exchange) throws Exception {
		exchange.startBlocking(); 
		exchange.dispatch(x -> {
			System.out.println(x.getIoThread().getName());
			standardHandler.get().handleRequest(x);
		});
	}
	
	public void setStandardHandler(HttpHandler standardHandler) {
		this.standardHandler.accept(standardHandler);
	}
	
	public void handleLivez(HttpServerExchange exchange) throws Exception {
		if (stateManager.isLive()) {
			exchange.setStatusCode(200);
			exchange.getResponseSender().send("ok");
		}
		else {
			exchange.setStatusCode(500);
			exchange.getResponseSender().send("fail");
		}
	}
	
	public void handleReadyz(HttpServerExchange exchange) throws Exception {
		if (stateManager.isReady()) {
			exchange.setStatusCode(200);
			exchange.getResponseSender().send("ready");
		}
		else {
			exchange.setStatusCode(503);
			exchange.getResponseSender().send("not ready");
		}
	}
}
