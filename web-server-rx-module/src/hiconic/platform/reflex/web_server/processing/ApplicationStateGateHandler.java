package hiconic.platform.reflex.web_server.processing;

import hiconic.rx.module.api.state.RxApplicationStateManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ApplicationStateGateHandler implements HttpHandler {
	private BlockingHolder<HttpHandler> standardHandler = new BlockingHolder<>();
	private RxApplicationStateManager stateManager;
	
	public ApplicationStateGateHandler(RxApplicationStateManager stateManager) {
		super();
		this.stateManager = stateManager;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getRelativePath();
		
		switch (path) {
		case "/livez" -> handleLivez(exchange);
		case "/readyz" -> handleReadyz(exchange);
		default -> standardHandler.get().handleRequest(exchange);
		}
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
