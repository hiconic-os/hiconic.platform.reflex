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
package hiconic.rx.web.rpc.client.api;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ModelReference;
import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.web.rpc.client.model.config.WebRpcClientConnection;

public interface WebRpcClientContract extends RxExportContract {

	Evaluator<ServiceRequest> remoteEvaluator(WebRpcClientConnection connection);
	ModelReference remoteServiceConfigurationModel(WebRpcClientConnection connection);
	ServiceProcessor<ServiceRequest, Object> remoteServiceProcessor(WebRpcClientConnection connection);
}
