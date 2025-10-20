// ============================================================================
package hiconic.rx.explorer.wire.space;

import java.util.concurrent.TimeUnit;

import com.braintribe.utils.system.SystemTools;
import com.braintribe.utils.system.exec.CommandExecutionImpl;
import com.braintribe.utils.system.exec.ProcessTerminatorImpl;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class SystemToolsSpace implements WireSpace {

	@Import
	private RxPlatformContract platform;

	public void startTasks() {
		platform.taskScheduler() //
				.scheduleAtFixedRate("Process-Terminator", processTerminator(), 0, 10, TimeUnit.SECONDS) //
				.interruptOnCancel(false) //
				.done();
	}

	@Managed
	public SystemTools systemTools() {
		SystemTools bean = new SystemTools();
		bean.setCommandExecution(commandExecution());
		return bean;
	}

	@Managed
	public CommandExecutionImpl commandExecution() {
		CommandExecutionImpl bean = new CommandExecutionImpl();
		bean.setProcessTerminator(processTerminator());
		return bean;
	}

	@Managed
	public ProcessTerminatorImpl processTerminator() {
		ProcessTerminatorImpl bean = new ProcessTerminatorImpl();
		return bean;
	}

}
