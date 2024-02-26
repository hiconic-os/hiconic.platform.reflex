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
