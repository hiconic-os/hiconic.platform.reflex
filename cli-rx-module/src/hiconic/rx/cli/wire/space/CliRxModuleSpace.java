package hiconic.rx.cli.wire.space;

import java.util.Arrays;

import com.braintribe.gm.cli.posix.parser.PosixCommandLineParser;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._CliApiModel_;
import hiconic.rx.cli.processing.CliExecutor;
import hiconic.rx.cli.processing.IntroductionProcessor;
import hiconic.rx.cli.processing.help.HelpProcessor;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.cli.model.api.Help;
import hiconic.rx.platform.cli.model.api.Introduce;

@Managed
public class CliRxModuleSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;
	
	@Import
	private RxProcessLaunchContract processLaunch;
	
	@Override
	public void addApiModels(ConfigurationModelBuilder builder) {
		builder.addDependency(_CliApiModel_.reflection);
	}
	
	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.register(Introduce.T, introductionProcessor());
		dispatching.register(Help.T, helpProcessor());
	}

	@Override
	public void onApplicationReady() {
		// start parsing, configuring and execution of cli inputs
		executor().process();
	}
	
	@Managed
	private HelpProcessor helpProcessor() {
		HelpProcessor bean = new HelpProcessor();
		bean.setCmdResolver(platform.mdResolver());
		bean.setLaunchScript(processLaunch.launchScriptName());
		return bean;
	}

	@Managed
	private IntroductionProcessor introductionProcessor() {
		IntroductionProcessor bean = new IntroductionProcessor();
		bean.setApplicationName(platform.applicationName());
		return bean;
	}
	
	@Managed
	private CliExecutor executor() {
		CliExecutor bean = new CliExecutor();
		bean.setArgs(processLaunch.cliArguments());
		bean.setParser(parser());
		bean.setDefaultDomains(Arrays.asList("main"));
		bean.setEvaluator(platform.evaluator());
		bean.setMarshallerRegistry(platform.marshallers());
		return bean;
	}
	
	@Managed
	private PosixCommandLineParser parser() {
		PosixCommandLineParser bean = new PosixCommandLineParser(domainId -> platform.mdResolver());
		return bean;
	}
}
