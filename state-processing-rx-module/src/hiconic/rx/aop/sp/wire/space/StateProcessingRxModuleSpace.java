package hiconic.rx.aop.sp.wire.space;

import java.util.List;
import java.util.function.Consumer;

import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateChangeProcessorRule;
import hiconic.rx.aop.sp.api.StateChangeProcessorRuleSet;
import hiconic.rx.aop.sp.api.StateProcessingContract;
import hiconic.rx.aop.sp.api.invocation.StateChangeProcessorInvocationPacket;
import hiconic.rx.aop.sp.aspect.StateProcessingAspect;
import hiconic.rx.aop.sp.commons.ConfigurableStateChangeProcessorRuleSet;
import hiconic.rx.aop.sp.invocation.multithreaded.MultiThreadedSpInvocation;
import hiconic.rx.aop.sp.registry.StateProcessorRegistryImpl;
import hiconic.rx.aop.sp.rule.MetaDataStateChangeProcessorRule;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.SecurityContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class StateProcessingRxModuleSpace implements RxModuleContract, StateProcessingContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private AccessContract access;

	@Import
	private SecurityContract security;

	@Override
	public AccessAspect createStateProcessingAspect() {
		return createStateProcessingAspect(List.of(metadataStateChangeProcessorRule()));
	}

	@Override
	public AccessAspect createStateProcessingAspect(List<StateChangeProcessorRule> rules) {
		ConfigurableStateChangeProcessorRuleSet ruleSet = ruleSet(rules);

		StateProcessingAspect bean = new StateProcessingAspect();
		bean.setProcessorRuleSet(ruleSet);
		bean.setAsyncInvocationQueue(asyncInvocationQueue(ruleSet));
		return bean;
	}

	private ConfigurableStateChangeProcessorRuleSet ruleSet(List<StateChangeProcessorRule> rules) {
		ConfigurableStateChangeProcessorRuleSet bean = new ConfigurableStateChangeProcessorRuleSet();
		bean.setProcessorRules(rules);
		return bean;
	}

	private Consumer<StateChangeProcessorInvocationPacket> asyncInvocationQueue(StateChangeProcessorRuleSet ruleSet) {
		MultiThreadedSpInvocation bean = new MultiThreadedSpInvocation();
		bean.setName("Aspect");
		bean.setExecutor(platform.execution().executorService());
		bean.setProcessorRuleSet(ruleSet);
		bean.setUserSessionScoping(security.userSessionScoping());
		bean.setSessionFactory(access.contextSessionFactory());
		bean.setSystemSessionFactory(access.systemSessionFactory());
		return bean;
	}

	@Managed
	private MetaDataStateChangeProcessorRule metadataStateChangeProcessorRule() {
		MetaDataStateChangeProcessorRule bean = new MetaDataStateChangeProcessorRule();
		bean.setRuleId("meta-data-scp-rule");
		bean.setStateProcessorRegistry(stateProcessorRegistry());

		return bean;
	}

	@Override
	public void registerStateChangeProcessor(StateChangeProcessor<?, ?> stateChangeProcessor) {
		stateProcessorRegistry().registerStateChangeProcessor(stateChangeProcessor);
	}

	@Override
	@Managed
	public StateProcessorRegistryImpl stateProcessorRegistry() {
		StateProcessorRegistryImpl bean = new StateProcessorRegistryImpl();

		return bean;
	}

}