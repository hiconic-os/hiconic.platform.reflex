// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.resource.RxPackagedResourceBuilder;
import hiconic.rx.module.api.resource.RxPackagedResourceInventory;
import hiconic.rx.module.api.wire.RxPackagedResourcesContract;
import hiconic.rx.platform.processing.resource.RxIndexedPackagedResourceResolver;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;
import hiconic.rx.resource.model.packaged.PackagedResourceNamespace;

/** Classpath-backed packaged resources. The contract permits adding a materialized-filesystem layer without changing consumers. */
@Managed
public class RxPackagedResourcesSpace implements RxPackagedResourcesContract {
	@Import private RxPlatformConfigContract config;

	@Override
	public RxPackagedResourceBuilder resource(String relativePath) {
		return resolver().resource(relativePath);
	}

	@Override
	public RxPackagedResourceInventory inventory() {
		return resolver().inventory();
	}

	@Managed
	private RxIndexedPackagedResourceResolver resolver() {
		return new RxIndexedPackagedResourceResolver(config.classpathIndex(), CLASSPATH_ROOT, PackagedResourceNamespace.resources);
	}
}
