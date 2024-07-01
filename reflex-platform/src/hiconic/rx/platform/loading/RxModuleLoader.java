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
package hiconic.rx.platform.loading;

import static com.braintribe.console.ConsoleOutputs.brightBlack;
import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.logging.Logger;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.space.ContractResolution;
import com.braintribe.wire.api.space.ContractSpaceResolver;
import com.braintribe.wire.api.space.WireSpace;
import com.braintribe.wire.impl.properties.PropertyLookups;

import hiconic.rx.module.api.wire.EnvironmentPropertiesContract;
import hiconic.rx.module.api.wire.RxContractSpaceResolverConfigurator;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.SystemPropertiesContract;
import hiconic.rx.platform.loading.RxModuleAnalysis.RxExportEntry;

public class RxModuleLoader implements LifecycleAware {

	private static final Logger logger = Logger.getLogger(RxModuleLoader.class);

	private WireContext<?> parentContext;
	private List<WireContext<RxModuleContract>> contexts;
	private RxContractSpaceResolverConfigurator resolverConfigurator;

	@SuppressWarnings("unused")
	private ExecutorService executorService;

	@Required
	public void setParentContext(WireContext<?> parentContext) {
		this.parentContext = parentContext;
	}

	@Required
	public void setContractSpaceResolverConfigurator(RxContractSpaceResolverConfigurator resolverConfigurator) {
		this.resolverConfigurator = resolverConfigurator;
	}

	@Required
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public Iterable<RxModuleContract> getModuleContracts() {
		return () -> contexts.stream() //
				.map(c -> c.contract()) //
				.iterator();
	}

	public List<RxModuleContract> listModuleContracts() {
		return contexts.stream() //
				.map(c -> c.contract()) //
				.toList();
	}

	@Override
	public void postConstruct() {
		println("Loading Modules:");
		loadModules();
		println();
	}

	@Override
	public void preDestroy() {
		closeWireModuleContexts();
	}

	private void loadModules() {
		Maybe<List<RxModule<?>>> maybeRxModules = WireModuleLoader.loadWireModules();

		RxModuleAnalysis rxAnalysis = RxModuleAnalyzer.analyze(maybeRxModules.get());

		Maybe<List<WireContext<RxModuleContract>>> contextsMaybe = loadWireModuleContexts(rxAnalysis);

		contexts = contextsMaybe.get();
	}

	private void closeWireModuleContexts() {
		for (WireContext<RxModuleContract> context : contexts)
			context.close();
	}

	private Maybe<List<WireContext<RxModuleContract>>> loadWireModuleContexts(RxModuleAnalysis analysis) {
		var contexts = new ArrayList<WireContext<RxModuleContract>>(analysis.nodes.size());

		var lazyError = new LazyInitialized<ConfigurationError>( //
				() -> ConfigurationError.create("Error while loading rx-module wire contexts"));

		var importResolver = new RxExportResolver(analysis.exports);

		resolverConfigurator.addResolver(importResolver);

		// TODO parallelize instead
		for (RxModuleNode node : nodesSortedDependenciesFirst(analysis)) {
			var maybeWireCtx = loadWireContextForModule(node);

			if (maybeWireCtx.isUnsatisfied())
				lazyError.get().getReasons().add(maybeWireCtx.whyUnsatisfied());
			else
				contexts.add(maybeWireCtx.get());
		}

		if (lazyError.isInitialized())
			return lazyError.get().asMaybe();

		return Maybe.complete(contexts);
	}

	private List<RxModuleNode> nodesSortedDependenciesFirst(RxModuleAnalysis analysis) {
		var rxNodes = new ArrayList<>(analysis.nodes.values());
		rxNodes.sort(Comparator.comparingInt(RxModuleNode::getIndex));
		return rxNodes;
	}

	private Maybe<WireContext<RxModuleContract>> loadWireContextForModule(RxModuleNode node) {
		var rxModule = node.module;

		printWireModule(rxModule.moduleName());

		try {
			WireContextBuilder<RxModuleContract> contextBuilder = Wire.contextBuilder(rxModule);
			for (RxExportEntry export : node.exports)
				if (export.spaceClass != null)
					contextBuilder.bindContract(export.contractClass, export.spaceClass);

			WireContext<RxModuleContract> wireContext = contextBuilder //
					.parent(parentContext) //
					.bindContracts(PropertyLookupContractResolver.INSTANCE) //
					.build();

			for (RxExportEntry export : node.exports)
				export.moduleWireContext = wireContext;

			return Maybe.complete(wireContext);

		} catch (Exception e) {
			logger.error("Error while loading module " + rxModule.moduleName());
			return Reasons.build(ConfigurationError.T) //
					.text("Could not load WireContext for WireModule " + rxModule.moduleName()) //
					.cause(InternalError.from(e)) //
					.toMaybe();
		}
	}

	private static void printWireModule(String wireModule) {
		int index = wireModule.lastIndexOf('.');

		String pckg = wireModule.substring(0, index);
		String name = wireModule.substring(index + 1);

		println( //
				sequence(text("  - "), //
						cyan(name), //
						brightBlack(" (" + pckg + ")") //
				));
	}

	public static Properties readApplicationProperties() {
		Enumeration<URL> resources;
		try {
			resources = RxModuleLoader.class.getClassLoader().getResources("META-INF/rx-app.properties");

		} catch (IOException e) {
			throw new UncheckedIOException("Could not determine META-INF/rx-app.properties class path resources", e);
		}

		var properties = new Properties();

		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			try (Reader reader = new InputStreamReader(url.openStream(), "UTF-8")) {
				properties.load(reader);

			} catch (IOException e) {
				throw new UncheckedIOException("Could not read properties from " + url, e);
			}
		}

		return properties;
	}

	private static class PropertyLookupContractResolver implements ContractSpaceResolver {

		public static final PropertyLookupContractResolver INSTANCE = new PropertyLookupContractResolver();

		@Override
		public ContractResolution resolveContractSpace(Class<? extends WireSpace> contractSpaceClass) {
			if (SystemPropertiesContract.class.isAssignableFrom(contractSpaceClass)) {
				return f -> PropertyLookups.create(contractSpaceClass, System::getProperty);
			}

			if (EnvironmentPropertiesContract.class.isAssignableFrom(contractSpaceClass)) {
				return f -> PropertyLookups.create(contractSpaceClass, System::getenv);
			}

			return null;
		}

	}

}
