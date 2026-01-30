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
package hiconic.rx.check.model.api.request;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.check.model.api.response.CheckResponse;

/** Executes {@link RunChecks} on the specified {@link #getNode() node}. */
public interface RunAimedChecks extends AuthorizedCheckRequest {

	EntityType<RunAimedChecks> T = EntityTypes.T(RunAimedChecks.class);

	@Mandatory
	String getNodeId();
	void setNodeId(String nodeId);

	@Override
	EvalContext<CheckResponse> eval(Evaluator<ServiceRequest> evaluator);

}
