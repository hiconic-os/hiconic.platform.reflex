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
package hiconic.rx.platform.cli.processing;

import java.util.function.Function;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

public class ValidationProcessor implements ReasonedServiceAroundProcessor<ServiceRequest, Object> {
	
	private Function<String, Validation> validationSupplier;
	
	@Required
	public void setValidationSupplier(Function<String, Validation> validationSupplier) {
		this.validationSupplier = validationSupplier;
	}
	
	@Override
	public Maybe<?> processReasoned(ServiceRequestContext context, ServiceRequest request,
			ProceedContext proceedContext) {
		ValidationProtocol protocol = validationSupplier.apply(context.getDomainId()).validate(request);
		
		if (protocol.hasViolations()) {
			InvalidArgument reason = InvalidArgument.create("Invalid Service Request");
			for (ConstraintViolation violation: protocol.getViolations()) {
				reason.getReasons().add(InvalidArgument.create( // 
					violation.getEntity().entityType().getShortName() + "." + violation.getProperty().getName() //
					+ " -> " + violation.getMessage()));
			}
			
			return reason.asMaybe();
		};
		
		return proceedContext.proceedReasoned(request);
	}
}
