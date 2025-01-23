package hiconic.platform.reflex.web_server.processing;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerEndpointConfig;

public class InstanceEndpointConfigurator extends ServerEndpointConfig.Configurator {
    private final Endpoint instance;

    public InstanceEndpointConfigurator(Endpoint instance) {
        this.instance = instance;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return endpointClass.cast(instance);
    }
}