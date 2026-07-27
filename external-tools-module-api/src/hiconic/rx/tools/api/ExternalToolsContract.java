package hiconic.rx.tools.api;

import com.braintribe.utils.system.exec.tool.ExternalToolRegistry;
import com.braintribe.utils.system.exec.tool.ToolExecutionEnvironment;

import hiconic.rx.module.api.wire.RxExportContract;

public interface ExternalToolsContract extends RxExportContract {

	ExternalToolRegistry tools();

	ToolExecutionEnvironment executionEnvironment();

}
