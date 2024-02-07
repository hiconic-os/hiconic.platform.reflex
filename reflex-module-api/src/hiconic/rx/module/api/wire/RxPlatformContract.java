package hiconic.rx.module.api.wire;

import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.wire.api.space.WireSpace;

/**
 * Wire contract that exposes general features of the reflex platform to be accessed by any reflex module. To access the features
 * you have to import this contract in your wire space. See example in the documentation of {@link RxModuleContract}.
 * 
 * @author dirk.scheffler
 *
 */
public interface RxPlatformContract extends WireSpace {
	/**
	 * Returns the service evaluator for the main service domain
	 */
	Evaluator<ServiceRequest> evaluator();

	/**
	 * Returns the {@link CmdResolver} for the main service domain 
	 */
	CmdResolver mdResolver();
	
	/**
	 * Returns the {@link ModelOracle} for the main service domain 
	 */
	ModelOracle modelOracle();
	
	/**
	 * Returns the {@link MarshallerRegistry}
	 */
	MarshallerRegistry marshallers();

	/**
	 *	Returns a configuration for the given type or a reason why the configuration could not be retrieved.
	 *	If an explicit configuration cannot be found a default initialized instance of the configType will be returned. 
	 */
	<C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType);
}
