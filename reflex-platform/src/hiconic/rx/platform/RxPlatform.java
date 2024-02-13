package hiconic.rx.platform;


import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.green;
import static com.braintribe.console.ConsoleOutputs.magenta;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;

import com.braintribe.console.AbstractAnsiConsole;
import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.model.generic.GMF;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.impl.properties.PropertyLookups;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.wire.RxPlatformWireModule;

public class RxPlatform {
	private static RxPlatformContract platformContract;
	private static Object monitor = new Object();
	private static ApplicationProperties properties = PropertyLookups.create(ApplicationProperties.class, RxModuleLoader.readApplicationProperties()::getProperty);
	
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		setupConsoleOutput();
		installShutdownHook();
		
		File appDir = determineAppDir();
		
		ConsoleOutputs.println(sequence( //
				text("Loading "), //
				magenta(properties.applicationName()), //
				text(" Application") //
		));
		
		platformContract = Wire.context(new RxPlatformWireModule(appDir, args, properties)).contract();

		long upTime = System.currentTimeMillis();
		long startupDuration = upTime - startTime;
		
		double startupDurationInS = startupDuration / 1000D;
		
		String formattedStartupDuration = String.format("%.3f", startupDurationInS);
		
		ConsoleOutputs.println(sequence(
				text("Application Loaded "),
				green("Successfully"),
				text(" in "),
				cyan(formattedStartupDuration + "s")
		));
		
		eagerLoading();
		
		try {
			synchronized(monitor) {
				monitor.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
		GMF.getTypeReflection().getPackagedModels().forEach(m -> m.getMetaModel());
	}

	private static File determineAppDir() {
		String appDir = System.getProperty("reflex.app.dir");
		
		if (appDir != null)
			return new File(appDir);
		
		return new File(".");
	}

	private static void setupConsoleOutput() {
		if (properties.consoleOutput())
			ConsoleConfiguration.install(new SysOutConsole(true));
	}
	
	private static class SysOutConsole extends AbstractAnsiConsole  {

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
