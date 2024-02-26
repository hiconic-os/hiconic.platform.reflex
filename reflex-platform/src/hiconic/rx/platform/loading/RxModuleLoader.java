package hiconic.rx.platform.loading;

import static com.braintribe.console.ConsoleOutputs.println;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
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
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.space.ContractResolution;
import com.braintribe.wire.api.space.ContractSpaceResolver;
import com.braintribe.wire.api.space.WireSpace;
import com.braintribe.wire.impl.properties.PropertyLookups;

import hiconic.rx.module.api.wire.EnvironmentPropertiesContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.SystemPropertiesContract;

public class RxModuleLoader implements LifecycleAware {

	private static final Logger logger = Logger.getLogger(RxModuleLoader.class);

	private WireContext<?> parentContext;
	private List<WireContext<RxModuleContract>> contexts;

	private ExecutorService executorService;

	@Required
	public void setParentContext(WireContext<?> parentContext) {
		this.parentContext = parentContext;
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
		Maybe<List<WireTerminalModule<RxModuleContract>>> wireModules = WireModuleLoader.loadWireModules();

		Maybe<List<WireContext<RxModuleContract>>> contextsMaybe = wireModules.flatMap(this::loadWireModuleContexts);

		contexts = contextsMaybe.get();
	}

	private void closeWireModuleContexts() {
		for (WireContext<RxModuleContract> context : contexts) {
			context.close();
		}
	}

	private Maybe<List<WireContext<RxModuleContract>>> loadWireModuleContexts(List<WireTerminalModule<RxModuleContract>> wireModules) {
		List<WireContext<RxModuleContract>> contexts = new ArrayList<>(wireModules.size());

		LazyInitialized<ConfigurationError> lazyError = new LazyInitialized<>(
				() -> ConfigurationError.create("Error while loading rx-module wire contexts"));

		for (WireTerminalModule<RxModuleContract> wireModule : wireModules) {
			Maybe<WireContext<RxModuleContract>> contextMaybe = loadWireModuleContext(wireModule);

			if (contextMaybe.isUnsatisfied()) {
				lazyError.get().getReasons().add(contextMaybe.whyUnsatisfied());
			}

			contexts.add(contextMaybe.get());
		}

		if (lazyError.isInitialized())
			return lazyError.get().asMaybe();

		return Maybe.complete(contexts);
	}

	private Maybe<WireContext<RxModuleContract>> loadWireModuleContext(WireTerminalModule<RxModuleContract> wireModule) {
		try {
			WireContext<RxModuleContract> wireContext = Wire.contextBuilder(wireModule) //
					.parent(parentContext) //
					.bindContracts(PropertyLookupContractResolver.INSTANCE).build();
			return Maybe.complete(wireContext);

		} catch (Exception e) {
			logger.error("Error while loading module " + wireModule);
			return Reasons.build(ConfigurationError.T) //
					.text("Could not load WireContext for WireModule " + wireModule.getClass().getName()) //
					.cause(InternalError.from(e)) //
					.toMaybe();
		}
	}

	public static Properties readApplicationProperties() {
		Enumeration<URL> resources;
		try {
			resources = RxModuleLoader.class.getClassLoader().getResources("META-INF/rx-app.properties");

		} catch (IOException e) {
			throw new UncheckedIOException("Could not determine META-INF/rx-app.properties class path resources", e);
		}

		Properties properties = new Properties();

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

		public static PropertyLookupContractResolver INSTANCE = new PropertyLookupContractResolver();

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
