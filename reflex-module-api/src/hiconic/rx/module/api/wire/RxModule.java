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
package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.tools.Generics;

/**
 * @see RxModuleContract
 */
public interface RxModule<M extends RxModuleContract> extends WireTerminalModule<RxModuleContract> {

	/**
	 * Binds {@link RxExportContract}s to the actual space.
	 *
	 * @see RxExportContract
	 * @see Exports
	 */
	@SuppressWarnings("unused")
	default void bindExports(Exports exports) {
		// implement if needed
	}

	default String moduleName() {
		return getClass().getName();
	}

	@Override
	default void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);

		Class<M> moduleSpaceClass = moduleSpaceClass();
		contextBuilder.bindContract(RxModuleContract.class, moduleSpaceClass);
	}

	default Class<M> moduleSpaceClass() {
		return (Class<M>) Generics.getGenericsParameter(getClass(), RxModule.class, "M");
	}

}
