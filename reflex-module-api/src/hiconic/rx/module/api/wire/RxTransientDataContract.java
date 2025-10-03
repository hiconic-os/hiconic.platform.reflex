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

import java.io.File;

import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.api.ResourceBuilder;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipeFactory;
import com.braintribe.wire.api.space.WireSpace;

/**
 * Various APIs related to transient data handling.
 */
public interface RxTransientDataContract extends WireSpace {

	/**
	 * A {@link StreamPipeFactory} for all-purpose {@link StreamPipe}s. Use this in the general case so the underlying resources can be shared most
	 * efficiently.
	 * <p>
	 * For more specific use-case (e.g. large number of small files or small number of large files) consider configuring your own factory instead.
	 */
	StreamPipeFactory streamPipeFactory();

	/** @return folder where {@link #streamPipeFactory()} stores it's temporary files. */
	File streamPipeFolder();

	/**
	 * Standard {@link ResourceBuilder} (click it to see usage example) for transient {@link Resource}s, most probably backed by
	 * {@link #streamPipeFactory()}.
	 */
	ResourceBuilder transientResourceBuilder();

}
