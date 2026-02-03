package hiconic.rx.check.wire.space;

import static com.braintribe.wire.api.util.Lists.list;

import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.thread.api.ThreadContextScope;
import com.braintribe.thread.impl.ThreadContextScopingImpl;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.check.api.CheckContract;
import hiconic.rx.check.api.CheckServiceDomain;
import hiconic.rx.check.model.api.request.CheckRequest;
import hiconic.rx.check.processing.BasicCheckProcessorRegistry;
import hiconic.rx.check.processing.CheckResponseHtmlMarshaller;
import hiconic.rx.check.processing.CheckRxProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class CheckRxModuleSpace implements RxModuleContract, CheckContract {

	@Import
	private RxPlatformContract platform;

	// ###############################################
	// ##. . . . . . . . Marshaller . . . . . . . . ##
	// ###############################################

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		configurator.marshallerRegistry().registerMarshaller("text/html;spec=check-response", checkResultToHtmlMarshaller());
	}

	@Managed
	@Override
	public Marshaller checkResultToHtmlMarshaller() {
		return new CheckResponseHtmlMarshaller();
	}

	// ###############################################
	// ## . . . . . . . Service Domain. . . . . . . ##
	// ###############################################

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configurations.byId(CheckServiceDomain.check) //
				.bindRequest(CheckRequest.T, this::checkRxProcessor);
	}

	@Managed
	private CheckRxProcessor checkRxProcessor() {
		CheckRxProcessor bean = new CheckRxProcessor();
		bean.setEvaluator(platform.systemEvaluator());
		bean.setInstanceId(platform.instanceId());
		bean.setThreadContextScoping(threadContextScoping());
		bean.setRegistry(checkProcessorRegistry());

		return bean;
	}

	@Override
	@Managed
	public BasicCheckProcessorRegistry checkProcessorRegistry() {
		BasicCheckProcessorRegistry bean = new BasicCheckProcessorRegistry();

		return bean;
	}

	// TODO move thread context scoping elsewhere
	// source: CurrentUserAuthContextSpace.threadContextScoping()
	@Managed
	private DeferringThreadContextScoping threadContextScoping() {
		ThreadContextScopingImpl bean = new ThreadContextScopingImpl();
		bean.setScopeSuppliers(list(serviceRequestContextThreadContextScopeSupplier()));
		return bean;
	}

	@Managed
	private StandardRequestContextThreadContextScopeSupplier serviceRequestContextThreadContextScopeSupplier() {
		StandardRequestContextThreadContextScopeSupplier bean = new StandardRequestContextThreadContextScopeSupplier();
		return bean;
	}

	public class StandardRequestContextThreadContextScopeSupplier implements Supplier<ThreadContextScope> {

		@Override
		public ThreadContextScope get() {

			AttributeContext callerContext = AttributeContexts.peek();

			return new ThreadContextScope() {
				@Override
				public void push() {
					AttributeContexts.push(callerContext);
				}

				@Override
				public void pop() {
					AttributeContexts.pop();
				}
			};
		}
	}

}

/* From: WebApiServerInitializer
 * 
 * registry.create("/healthz", RunHealthChecks.T, DdraUrlMethod.GET, null, "application/json", ACCESS_ID_CORTEX, Sets.set(DDRA_MAPPING_TAG_CHECKS),
 * configureReachabilityAndHighOutputPrettiness);
 * 
 * 
 * registry.create("/checkVitality", RunVitalityChecks.T, DdraUrlMethod.GET, null, "text/html;spec=check-response", ACCESS_ID_CORTEX,
 * Sets.set(DDRA_MAPPING_TAG_CHECKS), configureReachabilityAndHighOutputPrettiness);
 * 
 * 
 * HomePage : Runtime / Checks
 * 
 * registry.create("/check", RunChecks.T, DdraUrlMethod.GET, null, "text/html;spec=check-response", ACCESS_ID_CORTEX,
 * Sets.set(DDRA_MAPPING_TAG_CHECKS), configureReachabilityAndHighOutputPrettiness);
 * 
 * 
 * HomePage : Runtime / Health
 * 
 * 
 * registry.create("/checkDistributed", RunDistributedChecks.T, DdraUrlMethod.GET, null, "text/html;spec=check-response",
 * ACCESS_ID_CORTEX, Sets.set(DDRA_MAPPING_TAG_CHECKS), configureReachabilityAndHighOutputPrettiness);
 * 
 * 
 * registry.create("/checkAimed", RunAimedChecks.T, DdraUrlMethod.GET, null, "text/html;spec=check-response", ACCESS_ID_CORTEX,
 * Sets.set(DDRA_MAPPING_TAG_CHECKS), configureReachabilityAndHighOutputPrettiness);
 * 
 * 
 * 
 * HomePage : Runtime / Health
 * 
 * 
 * RunDistributedChecks run = session.create(RunDistributedChecks.T, "b9265418-8e97-4424-9e0a-32153bf0d715");
 * run.setAggregateBy(Lists.list(CrAggregationKind.node));
 * 
 * StaticPrototyping p = session.create(StaticPrototyping.T, "6d453267-7891-41a2-a649-31392e2b00d3"); p.setPrototype(run);
 * 
 * Consumer<DdraMapping> configureReachabilityAndHighOutputPrettinessAndRequestPrototype = configureReachabilityAndHighOutputPrettiness .andThen(m ->
 * { m.setRequestPrototyping(p); });
 * 
 * registry.create("/checkPlatform", RunDistributedChecks.T, DdraUrlMethod.GET, null, "text/html;spec=check-response", ACCESS_ID_CORTEX,
 * Sets.set(DDRA_MAPPING_TAG_CHECKS), configureReachabilityAndHighOutputPrettinessAndRequestPrototype); */