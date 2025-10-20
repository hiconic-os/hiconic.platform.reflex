// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.reflection.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

public interface CollectDiagnosticPackages extends PlatformReflectionRequest {

	EntityType<CollectDiagnosticPackages> T = EntityTypes.T(CollectDiagnosticPackages.class);

	Boolean getIncludeHeapDump();
	void setIncludeHeapDump(Boolean includeHeapDump);

	Boolean getIncludeLogs();
	void setIncludeLogs(Boolean includeLogs);

	@Description("If set to 'true', the response will omit binary data from the DCSA, i.e. it will exclude all CsaStoreResource operations.")
	boolean getExcludeSharedStorageBinaries();
	void setExcludeSharedStorageBinaries(boolean excludeSharedStorageBinaries);

	@Initializer("600000l") // 10 min
	@Name("Wait Timeout (ms)")
	@Description("The maximum amount of the time (in milliseconds) the server should wait for the collection of diagnostic packages. The default wait time is 10 min.")
	Long getWaitTimeoutInMs();
	void setWaitTimeoutInMs(Long waitTimeoutInMs);

	@Override
	EvalContext<? extends DiagnosticPackages> eval(Evaluator<ServiceRequest> evaluator);
}
