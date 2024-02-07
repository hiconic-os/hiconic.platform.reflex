package hiconic.rx.platform;


import java.io.File;

import com.braintribe.console.AbstractAnsiConsole;
import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.model.generic.GMF;
import com.braintribe.wire.api.Wire;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.wire.RxPlatformWireModule;

public class RxPlatform {
	private static RxPlatformContract platformContract;
	
	public static void main(String[] args) {
		setupConsoleOutput();
		
		File appDir = determineAppDir();
		
		ConsoleOutputs.println("Loading Application");
		platformContract = Wire.context(new RxPlatformWireModule(appDir)).contract();
		ConsoleOutputs.println("Application Loaded Successfully");
		
		eagerLoading();
		
		Object waiter = new Object();
		try {
			synchronized(waiter) {
				waiter.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
