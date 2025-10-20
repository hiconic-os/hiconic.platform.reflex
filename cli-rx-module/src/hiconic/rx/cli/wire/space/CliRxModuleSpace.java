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
package hiconic.rx.cli.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import com.braintribe.gm.cli.posix.parser.PosixCommandLineParser;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.cli.processing.CliExecutor;
import hiconic.rx.cli.processing.EntityFactory;
import hiconic.rx.cli.processing.FromEvaluator;
import hiconic.rx.cli.processing.IntroductionProcessor;
import hiconic.rx.cli.processing.ReflectServiceDomainsProcessor;
import hiconic.rx.cli.processing.help.CliServiceDomain;
import hiconic.rx.cli.processing.help.HelpProcessor;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.cli.model.api.Help;
import hiconic.rx.platform.cli.model.api.Introduce;
import hiconic.rx.platform.cli.model.api.Options;
import hiconic.rx.platform.cli.model.api.ReflectServiceDomains;

@Managed
public class CliRxModuleSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;

	@Import
	private RxProcessLaunchContract processLaunch;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration configuration = configurations.byId(CliServiceDomain.cli);

		configuration.bindRequest(Introduce.T, this::introductionProcessor);
		configuration.bindRequest(Help.T, this::helpProcessor);
		configuration.bindRequest(ReflectServiceDomains.T, this::reflectServiceDomainsProcessor);
	}

	@Override
	public void onApplicationReady() {
		// start parsing, configuring and execution of cli inputs
		executor().process();
	}

	@Managed
	private HelpProcessor helpProcessor() {
		HelpProcessor bean = new HelpProcessor();
		bean.setLaunchScript(processLaunch.launchScriptName());
		bean.setServiceDomains(platform.serviceDomains());
		return bean;
	}

	@Managed
	private ReflectServiceDomainsProcessor reflectServiceDomainsProcessor() {
		ReflectServiceDomainsProcessor bean = new ReflectServiceDomainsProcessor();
		bean.setServiceDomains(platform.serviceDomains());
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
		bean.setServiceDomains(platform.serviceDomains());
		bean.setDefaultDomains(platform.serviceDomains().list().stream().map(ServiceDomain::domainId).toList());
		bean.setServiceDomains(platform.serviceDomains());
		bean.setEvaluator(platform.evaluator());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setEntityFactory(inputEntityFactory());
		return bean;
	}

	@Managed
	private PosixCommandLineParser parser() {
		// TODO: better reasoning for type and domain lookup
		PosixCommandLineParser bean = new PosixCommandLineParser(this::cmdResolverForDomain);
		bean.setEntityFactory(inputEntityFactory());
		bean.setEntityEvaluator(fromEvaluator());
		return bean;
	}

	@Managed
	private FromEvaluator fromEvaluator() {
		FromEvaluator bean = new FromEvaluator();
		bean.setMarshallerRegistry(platform.marshallers());
		return bean;
	}
	@Managed
	private EntityFactory inputEntityFactory() {
		EntityFactory bean = new EntityFactory();
		bean.registerSingleton(Options.T, getOrTunnel(platform.readConfig(Options.T)));
		return bean;
	}

	private CmdResolver cmdResolverForDomain(String domainId) {
		ServiceDomain domain = platform.serviceDomains().byId(domainId);

		if (domain == null)
			return null;

		return domain.systemCmdResolver();
	}
}
