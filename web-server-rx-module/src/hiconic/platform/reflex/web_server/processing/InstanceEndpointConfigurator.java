// ============================================================================
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