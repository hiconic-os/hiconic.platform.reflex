package hiconic.rx.platform.service.wire.contract;

import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.platform.service.RxServiceDomain;

public interface RxServiceDomainContract extends WireSpace {

	CmdResolver cmdResolver();

	ModelOracle modelOracle();

	ConfigurableServiceRequestEvaluator evaluator();

	ConfigurationModelBuilder configurationModelBuilder();

	ConfigurableDispatchingServiceProcessor selectingServiceProcessor();

	RxServiceDomain serviceDomain();
}
