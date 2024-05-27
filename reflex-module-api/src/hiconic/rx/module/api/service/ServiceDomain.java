package hiconic.rx.module.api.service;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public interface ServiceDomain {
	/**
	 * The id that identifies this {@link ServiceDomain} internally and externally (e.g. can become part of a URL path)
	 */
	String domainId();

	/**
	 * The system {@link CmdResolver} taken from the associated {@link ConfiguredModel} via {@link ConfiguredModel#systemCmdResolver()}
	 */
	CmdResolver systemCmdResolver();
	
	/**
	 * The thread context associated {@link CmdResolver} taken from the associated {@link ConfiguredModel} via {@link ConfiguredModel#contextCmdResolver()}
	 */
	CmdResolver contextCmdResolver();

	/**
	 * A {@link CmdResolver} taken from the associated {@link ConfiguredModel} via {@link ConfiguredModel#cmdResolver(AttributeContext)}
	 */
	CmdResolver cmdResolver(AttributeContext attributeContext);

	/**
	 * The {@link ModelOracle} taken from the associated {@link ConfiguredModel} via {@link ConfiguredModel#modelOracle()}
	 */
	ModelOracle modelOracle();
	
	/**
	 * The {@link Evaluator} that is configured via the {@link #contextCmdResolver()} for {@link ServiceProcessor} bindings 
	 */
	Evaluator<ServiceRequest> evaluator();
	
	/**
	 * The {@link ServiceRequest} that should be executed for this domain if no {@link ServiceRequest} was supplied.
	 * An example use-case is, when a CLI application is started without any parameter and should execute a default request on a default {@link ServiceDomain}
	 */
	ServiceRequest defaultRequest();

}
