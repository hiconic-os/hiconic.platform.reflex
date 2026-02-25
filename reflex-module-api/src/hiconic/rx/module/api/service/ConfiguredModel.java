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
package hiconic.rx.module.api.service;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.module.api.wire.RxPlatformContract;

public interface ConfiguredModel extends ModelSymbol {

	/**
	 * {@link CmdResolver} contextualized with the system {@link AttributeContext} taken from
	 * {@link RxPlatformContract#systemAttributeContextSupplier()}
	 */
	CmdResolver systemCmdResolver();

	/**
	 * {@link CmdResolver} contextualized with the thread associated {@link AttributeContext} taken from {@link AttributeContexts#peek()}
	 */
	CmdResolver contextCmdResolver();

	/** {@link CmdResolver} contextualized with given {@link AttributeContext} */
	CmdResolver cmdResolver(AttributeContext attributeContext);

	/** {@link ModelOracle} for the underlying model. */
	ModelOracle modelOracle();
}
