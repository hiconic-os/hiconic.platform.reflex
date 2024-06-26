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
package hiconic.rx.web.rpc.client.wire.space;

import com.braintribe.gm._ServiceApiModel_;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.processing.webrpc.client.GmWebRpcRemoteServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.rpc.client.api.WebRpcClientContract;
import hiconic.rx.web.rpc.client.model.config.WebRpcClientConnection;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WebRpcClientRxModuleSpace implements RxModuleContract, WebRpcClientContract {

	@Import
	private RxPlatformContract platform;
	
	private ModelConfigurations modelConfigurations;

	@Override
	@Managed
	public Evaluator<ServiceRequest> remoteEvaluator(WebRpcClientConnection connection) {
		ConfigurableServiceRequestEvaluator bean = new ConfigurableServiceRequestEvaluator();
		bean.setServiceProcessor(remoteServiceProcessor(connection));
		bean.setExecutorService(platform.executorService());
		return bean;
	}
	
	@Override
	@Managed
	public ModelConfiguration remoteServiceConfigurationModel(WebRpcClientConnection connection) {
		ModelConfiguration bean = modelConfigurations.byName("configured-remote-service-api-model-" + connection.getName());
		
		bean.addModel(_ServiceApiModel_.reflection);
		bean.bindRequest(ServiceRequest.T, () -> remoteServiceProcessor(connection));
		
		return bean;
	}
	
	@Override
	@Managed
	public GmWebRpcRemoteServiceProcessor remoteServiceProcessor(WebRpcClientConnection connection) {
		GmWebRpcRemoteServiceProcessor bean = new GmWebRpcRemoteServiceProcessor();
		bean.setUrl(connection.getUrl());
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		return bean;
	}
	
	@Override
	public void configureModels(ModelConfigurations modelConfigurations) {
		this.modelConfigurations = modelConfigurations;
	}

}