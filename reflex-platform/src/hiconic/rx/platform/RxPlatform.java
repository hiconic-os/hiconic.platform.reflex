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
package hiconic.rx.platform;

import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.green;
import static com.braintribe.console.ConsoleOutputs.magenta;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.console.AbstractAnsiConsole;
import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.config.configurator.ClasspathConfigurator;
import com.braintribe.config.configurator.ConfiguratorContext;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.logging.level.LogLevelSetup;
import com.braintribe.ve.impl.StandardEnvironment;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.impl.properties.PropertyLookups;

import ch.qos.logback.classic.LoggerContext;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.conf.ApplicationProperties;
import hiconic.rx.platform.conf.ConfigurationPropertyImports;
import hiconic.rx.platform.conf.RxConfigurationConstants;
import hiconic.rx.platform.conf.RxPropertyResolver;
import hiconic.rx.platform.conf.SystemProperties;
import hiconic.rx.platform.configuration.ConfigurationImportDeclarations;
import hiconic.rx.platform.logging.LayeredLogbackConfiguration;
import hiconic.rx.platform.logging.LayeredLogLevelPersistence;
import hiconic.rx.platform.logging.LogbackLogLevelFramework;
import hiconic.rx.platform.logging.ProcessStandardStreams;
import hiconic.rx.platform.loading.RxModuleLoader;
import hiconic.rx.platform.loading.RxPropertiesLoader;
import hiconic.rx.platform.wire.RxPlatformWireModule;
import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;

public class RxPlatform implements AutoCloseable {
	private static final Logger logger = System.getLogger(RxPlatform.class.getName());

	private final SystemProperties systemProperties;
	private final ApplicationProperties applicationProperties;
	private final ClasspathIndex classpathIndex;
	private final RxPropertyResolver propertyResolver;
	private final Map<String, String> managedPropertyOverrides;

	private final String[] args;

	private ExtendedRxPlatformContract platformContract;

	private WireContext<RxPlatformContract> wireContext;

	private final boolean configureLogging;
	
	public RxPlatform() {
		this(new String[] {}, defaultSystemPropertyLookup(), defaultApplicationPropertyLookup(), Map.of(), false);
	}

	public RxPlatform(String[] args) {
		this(//
				args, //
				defaultSystemPropertyLookup(), //
				defaultApplicationPropertyLookup(), //
				Map.of(), //
				true //
		);
	}

	public RxPlatform(Function<String, String> systemPropertyLookup, Function<String, String> applicationPropertyLookup) {
		this(new String[] {}, systemPropertyLookup, applicationPropertyLookup, Map.of(), true);
	}

	public RxPlatform(String[] args, Function<String, String> systemPropertyLookup, Function<String, String> applicationPropertyLookup) {
		this(args, systemPropertyLookup, applicationPropertyLookup, Map.of(), true);
	}

	/**
	 * Starts a platform with explicit values added to the managed configuration property graph.
	 * <p>
	 * This is primarily useful to embedders and test harnesses. It does not restore the legacy implicit environment fallback.
	 */
	public RxPlatform(String[] args, Function<String, String> systemPropertyLookup, Function<String, String> applicationPropertyLookup,
			Map<String, String> managedPropertyOverrides) {
		this(args, systemPropertyLookup, applicationPropertyLookup, managedPropertyOverrides, true);
	}

	private RxPlatform(String[] args, Function<String, String> systemPropertyLookup, Function<String, String> applicationPropertyLookup,
			Map<String, String> managedPropertyOverrides, boolean configureLogging) {
		this.args = args;
		this.managedPropertyOverrides = Map.copyOf(managedPropertyOverrides);
		this.configureLogging = configureLogging;
		
		systemProperties = PropertyLookups.create(SystemProperties.class, systemPropertyLookup);
		applicationProperties = PropertyLookups.create(ApplicationProperties.class, applicationPropertyLookup);
		classpathIndex = createClasspathIndex();
		// Install Logback and the JUL bridge before configuration loading initializes
		// model reflection. Otherwise those early diagnostics escape through JUL's
		// default ConsoleHandler even when the application disables console logging.
		setupLogging();
		runClasspathConfigurators();
		propertyResolver = createPropertyResolver();

		start();
	}

	private void runClasspathConfigurators() {
		ConfiguratorContext context = new ConfiguratorContext("");
		context.setMaster(true);
		new ClasspathConfigurator(context).configure();
	}

	private ClasspathIndex createClasspathIndex() {
		String resourcesDir = systemProperties.packagedResourcesDir();
		if (resourcesDir == null || resourcesDir.isBlank())
			resourcesDir = systemProperties.classpathResourcesDir();
		if (resourcesDir == null || resourcesDir.isBlank())
			return new ClasspathIndex();

		List<ClasspathIndex.FilesystemSource> sources = new ArrayList<>();
		Path effectiveConfDir = systemProperties.appDir().toPath().resolve("effective-conf");
		if (Files.isDirectory(effectiveConfDir)) {
			sources.add(ClasspathIndex.filesystemSource(new File(resourcesDir).toPath(), "",
					List.of(RxConfigurationConstants.CLASSPATH_CONF_PATH)));
			sources.add(ClasspathIndex.filesystemSlots(effectiveConfDir, RxConfigurationConstants.CLASSPATH_CONF_PATH));
		} else {
			sources.add(ClasspathIndex.filesystemSource(new File(resourcesDir).toPath(), ""));

			// Compatibility with the first filesystem projection generation.
			Path packagedConfDir = systemProperties.appDir().toPath().resolve("packaged-conf");
			if (Files.isDirectory(packagedConfDir))
				sources.add(ClasspathIndex.filesystemSource(packagedConfDir, RxConfigurationConstants.CLASSPATH_CONF_PATH));
		}

		return new ClasspathIndex(sources);
	}

	public static Function<String, String> defaultSystemPropertyLookup() {
		return System::getProperty;
	}

	public static Function<String, String> defaultApplicationPropertyLookup() {
		return RxModuleLoader.readApplicationProperties()::getProperty;
	}

	public RxPlatformContract getContract() {
		return platformContract;
	}

	public WireContext<RxPlatformContract> getWireContext() {
		return wireContext;
	}

	public static void main(String[] args) {
		ProcessStandardStreams.initialize();
		ProcessStandardStreams.redirectConfigured();
		try {
			try (@SuppressWarnings("unused") RxPlatform platform = new RxPlatform(args)) {
				Object monitor = new Object();

				// Registering the shutdown hook
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					ConsoleOutputs.println("Shutting down Application");
					synchronized (monitor) {
						monitor.notify();
					}
				}));

				try {
					synchronized (monitor) {
						monitor.wait();
					}
				} catch (InterruptedException e) {
					logger.log(Level.ERROR, "Unexpected interruption", e);
				}
			}
			catch (UnsatisfiedMaybeTunneling e) {
				String msg = "Error while starting application:\n" + e.getMaybe().whyUnsatisfied().stringify();
				logger.log(Level.ERROR, msg, e);
				System.err.println(msg);
			}
			catch (ReasonException e) {
				String msg = "Error while starting application:\n" + e.getReason().stringify();
				logger.log(Level.ERROR, msg, e);
				System.err.println(msg);
			}
			catch (Exception e) {
				String msg = "Error while starting application";
				// Handle errors during configuration
				logger.log(Level.ERROR, msg, e);

				System.err.println(msg);
				e.printStackTrace(System.err);
			}
		} finally {
			ProcessStandardStreams.restore();
		}
	}

	private void start() {
		long startTime = System.currentTimeMillis();
		setupLogLevels();
		setupConsoleOutput();

		ConsoleOutputs.println(sequence( //
				text("Loading "), //
				magenta(applicationProperties.applicationName()), //
				text(" Application") //
		));

		wireContext = Wire.context(new RxPlatformWireModule(args, applicationProperties, systemProperties, classpathIndex, propertyResolver));
		platformContract = wireContext.contract(ExtendedRxPlatformContract.class);

		long upTime = System.currentTimeMillis();
		long startupDuration = upTime - startTime;

		double startupDurationInS = startupDuration / 1000D;

		String formattedStartupDuration = String.format("%.3f", startupDurationInS);

		ConsoleOutputs.println(sequence( //
				text("Application Loaded "), //
				green("Successfully"), //
				text(" in "), //
				cyan(formattedStartupDuration + "s") //
		));

		String domainIds = platformContract.serviceProcessing().serviceDomains().list().stream() //
				.map(RxPlatform::formatServiceDomain) //
				.sorted() //
				.collect(Collectors.joining("\n\t"));
		ConsoleOutputs.println(sequence( //
				text("Service Domains:\n\t"), //
				cyan(domainIds)) //
		);

		eagerLoading();

		logger.log(Level.INFO, "Application loaded");

	}

	private static String formatServiceDomain(ServiceDomain domain) {
		if (domain.aliases().isEmpty())
			return domain.domainId();

		String aliases = domain.aliases().stream().sorted().collect(Collectors.joining(", "));
		return domain.domainId() + " (aliases: " + aliases + ")";
	}

	@Override
	public void close() {
		platformContract.onApplicationShutdown();
		wireContext.close();
	}

	private void setupLogging() {
		if (!configureLogging)
			return;

		if (!applicationProperties.setupLogging())
			return;

		// Assume SLF4J is bound to logback in the current environment
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		File confDir = new File(systemProperties.appDir(), "conf");

		try {
			new LayeredLogbackConfiguration(classpathIndex, RxConfigurationConstants.CLASSPATH_CONF_PATH, confDir).configure(context);
		} catch (Exception e) {
			System.err.print("Error configuring Logback: ");
			e.printStackTrace(System.err);
		}

		// Remove existing handlers attached to the j.u.l root logger
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		// Add SLF4JBridgeHandler to j.u.l's root logger
		SLF4JBridgeHandler.install();
	}

	private void setupLogLevels() {
		File confDir = new File(systemProperties.appDir(), "conf");

		LogLevelSetup setup = new LogLevelSetup();
		setup.setConfDir(confDir);
		setup.setLogLevelFramework(new LogbackLogLevelFramework());
		setup.setPackagedLogLevelPersistence(
				new LayeredLogLevelPersistence(classpathIndex, RxConfigurationConstants.CLASSPATH_CONF_PATH, confDir, propertyResolver::resolve));
		setup.setPropertyLookup(propertyResolver::resolve);
		LogLevelSetup.setInstance(setup);

		setup.applyEffectiveLogLevels();
	}

	private RxPropertyResolver createPropertyResolver() {
		RxPropertyResolver resolver = new RxPropertyResolver();
		File confDir = new File(systemProperties.appDir(), "conf");
		Map<String, String> rawProperties = UnsatisfiedMaybeTunneling.getOrTunnel(
				RxPropertiesLoader.loadLayered(confDir, RxConfigurationConstants.CLASSPATH_CONF_PATH, "properties", new YamlMarshaller(), classpathIndex));
		rawProperties = new LinkedHashMap<>(rawProperties);
		rawProperties.putIfAbsent(SystemProperties.PROPERTY_APP_DIR, systemProperties.appDir().getPath());
		rawProperties.putAll(managedPropertyOverrides);

		ConfigurationImportDeclarations imports = UnsatisfiedMaybeTunneling.getOrTunnel(
				ConfigurationImportDeclarations.read(classpathIndex));
		if (imports.declaredConfiguration()) {
			rawProperties = UnsatisfiedMaybeTunneling.getOrTunnel(
					ConfigurationPropertyImports.bind(rawProperties, imports, StandardEnvironment.INSTANCE));
			resolver.setManagedPropertiesOnly(true);
		}

		resolver.setRawProperties(rawProperties);
		return resolver;
	}
	
	private void eagerLoading() {
		// GMF.getTypeReflection().getPackagedModels().forEach(m -> m.getMetaModel());
	}

	private void setupConsoleOutput() {
		if (applicationProperties.consoleOutput())
			ConsoleConfiguration.install(new SysOutConsole(true));
	}

	private static class SysOutConsole extends AbstractAnsiConsole {

		public SysOutConsole(boolean ansiConsole) {
			super(ansiConsole, false);
		}

		@Override
		protected void _out(CharSequence text, boolean linebreak) {
			if (linebreak)
				System.out.println(text);
			else
				System.out.print(text);

			System.out.flush();
		}
	}
}
