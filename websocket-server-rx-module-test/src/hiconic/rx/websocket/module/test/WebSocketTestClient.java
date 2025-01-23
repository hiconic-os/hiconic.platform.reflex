package hiconic.rx.websocket.module.test;

import java.net.URI;
import java.util.function.Consumer;

import com.braintribe.processing.async.impl.HubPromise;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

public class WebSocketTestClient implements MessageHandler.Whole<String>, AutoCloseable {
	private Session session;
	private Consumer<String> dataConsumer;
	public WebSocketTestClient(int port, Consumer<String> dataConsumer, HubPromise<Boolean> established) throws Exception {
		this.dataConsumer = dataConsumer;
		String serverUri = "ws://localhost:" + port + "/ws?clientId=test&accept=application/json&sendChannelId=true"; 
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();

		session = container.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(WebSocketTestClient.this);
				established.accept(true);
			}
			
			@Override
			public void onError(Session session, Throwable thr) {
				established.accept(false);
			}
			
			@Override
			public void onClose(Session session, CloseReason closeReason) {
			}

		}, config, URI.create(serverUri));			

	}
	
	@Override
	public void onMessage(String message) {
		dataConsumer.accept(message);
	}
	
	@Override
	public void close() throws Exception {
		session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "bye bye"));
	}
}
