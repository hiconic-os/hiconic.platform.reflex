package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.space.WireSpace;

/**
 * Wire contract that exposes informations about the way the reflex platform process was launched
 * 
 * @author dirk.scheffler
 *
 */
public interface RxProcessLaunchContract extends WireSpace {
	
	/**
	 * The arguments that were passed to the platform's main method
	 */
	String[] cliArguments();
	
	/**
	 * The name of the launch script (bat/sh) that started the process
	 */
	String launchScriptName();

}
