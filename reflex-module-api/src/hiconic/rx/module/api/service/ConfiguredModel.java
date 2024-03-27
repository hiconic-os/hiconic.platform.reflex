package hiconic.rx.module.api.service;

import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;

public interface ConfiguredModel extends ConfiguredModelReference {
	CmdResolver cmdResolver();
	ModelOracle modelOracle();
}
