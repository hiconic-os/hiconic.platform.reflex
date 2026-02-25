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
package hiconic.rx.platform.wire;

import java.io.File;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.module.api.wire.RxContractSpaceResolverConfigurator;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.module.api.wire.RxTransientDataContract;
import hiconic.rx.platform.conf.ApplicationProperties;
import hiconic.rx.platform.conf.SystemProperties;
import hiconic.rx.platform.loading.RxConfigurableContractSpaceResolver;
import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;
import hiconic.rx.platform.wire.space.RxAuthSpace;
import hiconic.rx.platform.wire.space.RxPlatformResourcesSpace;
import hiconic.rx.platform.wire.space.RxPlatformSpace;
import hiconic.rx.platform.wire.space.RxTransientDataSpace;

public class RxPlatformWireModule implements WireTerminalModule<RxPlatformContract> {
	
	private RxPlatformConfigContract config;
	private final RxConfigurableContractSpaceResolver configurableContractSpaceResolver = new RxConfigurableContractSpaceResolver();
	
	public RxPlatformWireModule(String[] cliArguments, ApplicationProperties applicationProperties, SystemProperties systemProperties) {
		super();
		
		config = new RxPlatformConfigContract() {
			
			@Override
			public ApplicationProperties properties() {
				return applicationProperties;
			}
			
			@Override
			public File appDir() {
				return systemProperties.appDir();
			}
			
			@Override
			public String launchScriptName() {
				return systemProperties.launchScript();
			}
			
			@Override
			public String[] cliArguments() {
				return cliArguments;
			}
			
			@Override
			public RxContractSpaceResolverConfigurator contractSpaceResolverConfigurator() {
				return configurableContractSpaceResolver;
			}
		};
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxPlatformContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxAuthContract.class, RxAuthSpace.class);
		contextBuilder.bindContract(ExtendedRxPlatformContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxPlatformResourcesContract.class, RxPlatformResourcesSpace.class);
		contextBuilder.bindContract(RxTransientDataContract.class, RxTransientDataSpace.class);
		contextBuilder.bindContract(RxProcessLaunchContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxPlatformConfigContract.class, config);
		contextBuilder.bindContracts(configurableContractSpaceResolver); // here RxExportContracts are registered and can be resolved
	}

}
