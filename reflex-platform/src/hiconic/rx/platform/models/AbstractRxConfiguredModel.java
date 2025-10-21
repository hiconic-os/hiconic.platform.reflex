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
package hiconic.rx.platform.models;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.Lazy;

import hiconic.rx.module.api.service.ConfiguredModel;

public abstract class AbstractRxConfiguredModel implements ConfiguredModel {

	protected final Lazy<ModelOracle> lazyModelOracle = new Lazy<>(this::buildModelOracle);
	protected final Lazy<CmdResolver> lazySystemCmdResolver = new Lazy<>(this::buildSystemCmdResolver);
	protected final RxConfiguredModels configuredModels;

	protected AbstractRxConfiguredModel(RxConfiguredModels configuredModels) {
		this.configuredModels = configuredModels;
		
	}
	
	protected abstract ModelOracle buildModelOracle();
	
	private CmdResolver buildSystemCmdResolver() {
		return configuredModels.systemCmdResolver(modelOracle());
	}
	
	@Override
	public CmdResolver systemCmdResolver() {
		return lazySystemCmdResolver.get();
	}
	
	@Override
	public CmdResolver cmdResolver(AttributeContext attributeContext) {
		return configuredModels.cmdResolver(attributeContext, modelOracle());
	}
	
	@Override
	public CmdResolver contextCmdResolver() {
		return cmdResolver(AttributeContexts.peek());
	}

	@Override
	public ModelOracle modelOracle() {
		return lazyModelOracle.get();
	}

}
