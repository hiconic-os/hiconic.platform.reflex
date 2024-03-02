package hiconic.rx.platform;

import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.green;
import static com.braintribe.console.ConsoleOutputs.magenta;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.braintribe.console.AbstractAnsiConsole;
import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.impl.properties.PropertyLookups;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import hiconic.rx.platform.conf.ApplicationProperties;
import hiconic.rx.platform.conf.SystemProperties;
import hiconic.rx.platform.loading.RxModuleLoader;
import hiconic.rx.platform.wire.RxPlatformWireModule;

public class RxPlatform {

	private static final Object monitor = new Object();

	private static final SystemProperties systemProperties = PropertyLookups.create(SystemProperties.class, System::getProperty);
	private static final ApplicationProperties applicationProperties = PropertyLookups.create( //
			ApplicationProperties.class, RxModuleLoader.readApplicationProperties()::getProperty);
	
	private static final Logger logger = System.getLogger(RxPlatform.class.getName());

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		setupLogging();
		setupConsoleOutput();
		installShutdownHook();

		ConsoleOutputs.println(sequence( //
				text("Loading "), //
				magenta(applicationProperties.applicationName()), //
				text(" Application") //
		));

		Wire.context(new RxPlatformWireModule(args, applicationProperties, systemProperties)).contract();

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

		eagerLoading();
		
		logger.log(Level.INFO, "Application loaded");

		try {
			synchronized (monitor) {
				monitor.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void setupLogging() {
		// Assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        File logConfig = new File(systemProperties.appDir(), "conf/logback.xml");
        
        if (logConfig.exists()) {
	        try {
	            JoranConfigurator configurator = new JoranConfigurator();
	            configurator.setContext(context);
	            // Clear any previous configuration
	            context.reset(); 
	            // Load new configuration
	            configurator.doConfigure(logConfig);
	        } catch (Exception e) {
	            // Handle errors during configuration
	            System.err.print("Error configuring Logback: ");
	            e.printStackTrace(System.err);
	        }
        }
		
		// Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        // Add SLF4JBridgeHandler to j.u.l's root logger
        SLF4JBridgeHandler.install();
	}

	private static void installShutdownHook() {
		// Registering the shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ConsoleOutputs.println("Shutting down Application");
			synchronized (monitor) {
				monitor.notify();
			}
		}));
	}

	private static void eagerLoading() {
		// GMF.getTypeReflection().getPackagedModels().forEach(m -> m.getMetaModel());
	}

	private static void setupConsoleOutput() {
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
