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
package hiconic.rx.check.model.api.response;

import java.util.Set;

import com.braintribe.model.descriptive.HasName;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.result.Failure;

import hiconic.rx.check.model.aspect.CheckCoverage;
import hiconic.rx.check.model.aspect.CheckLatency;
import hiconic.rx.check.model.result.CheckResult;

public interface CrCheckResult extends HasName, CrAggregatable {

	EntityType<CrCheckResult> T = EntityTypes.T(CrCheckResult.class);

	CheckResult getResult();
	void setResult(CheckResult result);

	String getCheckProcessorName();
	void setCheckProcessorName(String checkProcessorName);

	String getNode();
	void setNode(String node);

	Failure getProcessingFailure();
	void setProcessingFailure(Failure processingFailure);

	CheckCoverage getCoverage();
	void setCoverage(CheckCoverage coverage);

	CheckLatency getLatency();
	void setLatency(CheckLatency weight);

	Set<String> getLabels();
	void setLabels(Set<String> labels);

	
	@Override
	default boolean isResult() {
		return true;
	}

}
