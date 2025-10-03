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
package hiconic.rx.platform.wire.space;

import java.io.File;

import com.braintribe.model.resource.api.ResourceBuilder;
import com.braintribe.model.resource.utils.StreamPipeTransientResourceBuilder;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.stream.pools.CompoundBlockPool;
import com.braintribe.utils.stream.pools.SmartBlockPoolFactory;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxTransientDataContract;

/**
 * 
 */
@Managed
public class RxTransientDataSpace implements RxTransientDataContract {

	/** {@inheritDoc} */
	@Override
	@Managed
	public ResourceBuilder transientResourceBuilder() {
		return new StreamPipeTransientResourceBuilder(streamPipeFactory());
	}

	/** {@inheritDoc} */
	@Override
	@Managed
	public CompoundBlockPool streamPipeFactory() {
		SmartBlockPoolFactory poolFactory = SmartBlockPoolFactory.usingAvailableMemory(0.1);
		poolFactory.setStreamPipeFolder(streamPipeFolder());

		return poolFactory.create();
	}

	@Override
	@Managed
	public File streamPipeFolder() {
		File tempDir = FileTools.getTempDir();
		File bean = new File(tempDir, "platform/basicStreamPipe");

		return bean;
	}

}
