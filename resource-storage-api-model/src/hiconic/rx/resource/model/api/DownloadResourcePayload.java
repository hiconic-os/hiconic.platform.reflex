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
package hiconic.rx.resource.model.api;

import java.util.Date;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resourceapi.stream.HasStreamCondition;
import com.braintribe.model.resourceapi.stream.HasStreamRange;
import com.braintribe.model.resourceapi.stream.condition.FingerprintMismatch;
import com.braintribe.model.resourceapi.stream.condition.ModifiedSince;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface DownloadResourcePayload extends ExistingResourcePayloadRequest, HasStreamCondition, HasStreamRange {

	EntityType<DownloadResourcePayload> T = EntityTypes.T(DownloadResourcePayload.class);

	// WHY THESE TWO PROPERTIES?

	// The original implementation for BinaryRetrievalRequest was so dumb it had a Resource passed to it and a Condition.
	// and it was comparing the condition value (e.g. md5) to the passed Resource value, i.e. comparing two values from the request.
	// I am keeping it that way for now, but has to be addressed.

	/** Value of {@link Resource#getMd5()} to evaluate {@link FingerprintMismatch} condition. */
	String getMd5();
	void setMd5(String md5);

	/** Value of {@link Resource#getCreated()} to evaluate {@link ModifiedSince} condition. */
	Date getCreated();
	void setCreated(Date created);

	@Override
	EvalContext<? extends DownloadResourcePayloadResponse> eval(Evaluator<ServiceRequest> evaluator);

}
