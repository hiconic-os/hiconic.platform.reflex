package hiconic.rx.platform;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxModuleContract;

public class RxModuleLoader implements LifecycleAware {
	private static final Logger logger = Logger.getLogger(RxModuleLoader.class);
	private WireContext<?> parentContext;
	private List<WireContext<? extends RxModuleContract>> contexts;
	private ConfigurableDispatchingServiceProcessor dispatching;
	private ConfigurationModelBuilder configurationModelBuilder;
	
	@Required
	public void setParentContext(WireContext<?> parentContext) {
		this.parentContext = parentContext;
	}
	
	public Iterable<RxModuleContract> getModuleContracts() {
		return () -> contexts.stream().map(c -> (RxModuleContract)c.contract()).iterator();
	}
	
	@Override
	public void postConstruct() {
		loadModules();
	}
	
	@Override
	public void preDestroy() {
		closeWireModuleContexts();
	}
	
	private void loadModules() {
		Maybe<List<WireContext<? extends RxModuleContract>>> contextsMaybe = loadWireModules() //
			.flatMap(this::loadWireModuleContexts);
		
		contexts = contextsMaybe.get();
	}
	
	private void closeWireModuleContexts() {
		for (WireContext<? extends RxModuleContract> context: contexts) {
			context.close();
		}
	}
	
	public Maybe<List<WireTerminalModule<? extends RxModuleContract>>> loadWireModules() {
		try {
			Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/rx-module.properties");
			
			LazyInitialized<ConfigurationError> lazyError = new LazyInitialized<>( //
					() -> ConfigurationError.create("Error while loading rx-module configurations"));
			
			List<WireTerminalModule<? extends RxModuleContract>> wireModules = new ArrayList<>();
			
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				Maybe<WireTerminalModule<? extends RxModuleContract>> wireModuleMaybe = loadWireModule(url);
				
				if (wireModuleMaybe.isUnsatisfied())
					lazyError.get().getReasons().add(wireModuleMaybe.whyUnsatisfied());
				
				wireModules.add(wireModuleMaybe.get());
			}
			
			if (lazyError.isInitialized())
				return lazyError.get().asMaybe();
			
			return Maybe.complete(wireModules);
			
		} catch (IOException e) {
			return Reasons.build(IoError.T) //
					.text("Could not enumerate classpath resources with the name 'META-INF/rx-module.properties'") //
					.cause(InternalError.from(e)) //
					.toMaybe();
		}
	}
	
	public Maybe<WireTerminalModule<? extends RxModuleContract>> loadWireModule(URL propertiesUrl) {
		Properties properties = new Properties();
		
		try (Reader reader = new InputStreamReader(propertiesUrl.openStream(), "UTF-8")) {
			properties.load(reader);
		} catch (IOException e) {
			return Reasons.build(IoError.T) //
					.text("Could not read properties from " + propertiesUrl) //
					.cause(InternalError.from(e)) //
					.toMaybe();
		}
		
		String wireModule = properties.getProperty("wire-module");
		
		
		if (wireModule == null)
			return Reasons.build(ConfigurationError.T).text("Missing wire-module property in " + propertiesUrl).toMaybe();
		
		// TODO: more dynamic reflection of loading 
		ConsoleOutputs.println("Loading Module: " + wireModule);
		
		Class<?> wireModuleClass;
		try {
			wireModuleClass = Class.forName(wireModule);
		} catch (ClassNotFoundException e) {
			return Reasons.build(ConfigurationError.T).text("Could not find class " + wireModule +" configured with wire-module property in " + propertiesUrl).toMaybe();
		}
		
		if (!wireModuleClass.isEnum())
			return Reasons.build(ConfigurationError.T).text("Class " + wireModule +" configured with wire-module property in " + propertiesUrl + " is not an enum class").toMaybe();
		
		var enumClass = (Class<? extends Enum>)wireModuleClass;

		Enum<?> constant;
		
		try {
			constant = Enum.valueOf(enumClass, "INSTANCE");
		} catch (IllegalArgumentException e) {
			return Reasons.build(ConfigurationError.T).text("Enum class " + wireModule +" configured with wire-module property in " + propertiesUrl + " is missing a constant INSTANCE").toMaybe();
		}
		
		if (!(constant instanceof WireTerminalModule))
			return Reasons.build(NotFound.T).text("Constant INSTANCE of enum class " + wireModule +" configured with wire-module property in " + propertiesUrl + " is not a WireTerminalModule").toMaybe();
		
		var wireTerminalModule = (WireTerminalModule<? extends RxModuleContract>) constant;
		
		if (!RxModuleContract.class.isAssignableFrom(wireTerminalModule.contract())) {
			return Reasons.build(ConfigurationError.T).text("Constant INSTANCE of enum class " + wireModule +" configured with wire-module property in " + propertiesUrl + " is not a WireTerminalModule with a contract of type RxModuleContract").toMaybe();
		}
		
		return Maybe.complete(wireTerminalModule);
			
	}
	
	private Maybe<List<WireContext<? extends RxModuleContract>>> loadWireModuleContexts(List<WireTerminalModule<? extends RxModuleContract>> wireModules) {
		List<WireContext<? extends RxModuleContract>> contexts = new ArrayList<>(wireModules.size());
		
		LazyInitialized<ConfigurationError> lazyError = new LazyInitialized<>(() -> ConfigurationError.create("Error while loading rx-module wire contexts"));
		
		for (WireTerminalModule<? extends RxModuleContract> wireModule: wireModules) {
			Maybe<WireContext<? extends RxModuleContract>> contextMaybe = loadWireModuleContext(wireModule);
			
			if (contextMaybe.isUnsatisfied()) {
				lazyError.get().getReasons().add(contextMaybe.whyUnsatisfied());
			}
			
			contexts.add(contextMaybe.get());
		}
		
		if (lazyError.isInitialized())
			return lazyError.get().asMaybe();
		
		return Maybe.complete(contexts);
	}
	
	private Maybe<WireContext<? extends RxModuleContract>> loadWireModuleContext(WireTerminalModule<? extends RxModuleContract> wireModule) {
		try {
			WireContext<? extends RxModuleContract> wireContext = Wire.contextBuilder(wireModule).parent(parentContext).build();
			return Maybe.complete(wireContext);
		} catch (Exception e) {
			logger.error("Error while loading module " + wireModule);
			return Reasons.build(ConfigurationError.T).text("Could not load WireContext for WireModule " + wireModule.getClass().getName()) //
					.cause(InternalError.from(e)).toMaybe();
		}
	}
}
