package hiconic.rx.module.api.service;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.service.api.ServiceRequest;

public interface ServiceDomain {
	String domainId();
	CmdResolver cmdResolver();
	ModelOracle modelOracle();
	Evaluator<ServiceRequest> evaluator();
	ServiceRequest defaultRequest();
}
