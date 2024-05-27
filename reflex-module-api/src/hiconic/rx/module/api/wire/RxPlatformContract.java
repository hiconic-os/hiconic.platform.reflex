package hiconic.rx.module.api.wire;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ServiceDomains;

/**
 * Wire contract that exposes general features of the reflex platform to be accessed by any reflex module. To access the features you have to import
 * this contract in your wire space. See example in the documentation of {@link RxModuleContract}.
 * 
 * @author dirk.scheffler
 */
public interface RxPlatformContract extends WireSpace {

	/** Standard request {@link Evaluator}. */
	Evaluator<ServiceRequest> evaluator();

	/**
	 * {@link Evaluator} backed by {@link #systemAttributeContextSupplier()}, thus having system user (i.e. full) authorization when evaluating
	 * requests.
	 */
	Evaluator<ServiceRequest> systemEvaluator();

	/** {@link Evaluator} backed by given {@link AttributeContext}, which can e..g. */
	Evaluator<ServiceRequest> evaluator(AttributeContext attributeContext);

	/** {@link AttributeContext} equipped with the system user's {@link UserSession}. */
	Supplier<AttributeContext> systemAttributeContextSupplier();

	/** Returns the {@link ConfiguredModels}. */
	ConfiguredModels configuredModels();

	/** Returns the {@link ServiceDomains}. */
	ServiceDomains serviceDomains();

	/** Returns the {@link MarshallerRegistry} */
	MarshallerRegistry marshallers();

	/** The name of the application which the platform is hosting given by the applicationName property in META-INF/rx-app.properties */
	String applicationName();

	/** The nodeId of this instance in distributed systems */
	String nodeId();

	/**
	 * Returns a configuration for the given type or a reason why the configuration could not be retrieved.
	 * <p>
	 * If an explicit configuration cannot be found a default initialized instance of the configType will be returned.
	 */
	<C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType);

	/** General purpose thread pool. */
	ExecutorService executorService();

}
