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
package hiconic.rx.explorer.wire.space;

import java.util.ArrayList;
import java.util.List;

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.platform.reflex._ExplorerCortexModel_;
import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.explorer.model.configuration.access.ReadOnlySmoodAccess;
import hiconic.rx.explorer.model.cortex.access.RxIncrementalAccess;
import hiconic.rx.explorer.processing.access.StaticNonIncrementalAccess;

/**
 * @author peter.gazdik
 */
@Managed
public class CortexSpace implements WireSpace {

	// @formatter:off
	@Import private AccessContract access;
	// @formatter:on

	public void registerCortexAccess() {
		access.deploy(cortexDenotation(), cortexAccess());
	}

	@Managed
	private ReadOnlySmoodAccess cortexDenotation() {
		ReadOnlySmoodAccess bean = ReadOnlySmoodAccess.T.create();
		bean.setAccessId("cortex");
		bean.setDataModelName(_ExplorerCortexModel_.name);

		return bean;
	}

	@Managed
	private hiconic.rx.explorer.processing.access.ReadOnlySmoodAccess cortexAccess() {
		StaticNonIncrementalAccess dataDelegate = new StaticNonIncrementalAccess();
		dataDelegate.setModelSupplier(() -> cortexGmModel());
		dataDelegate.setDataSupplier(() -> cortexData());

		hiconic.rx.explorer.processing.access.ReadOnlySmoodAccess bean = new hiconic.rx.explorer.processing.access.ReadOnlySmoodAccess();
		bean.setAccessId("cortex");
		bean.setModelName(_ExplorerCortexModel_.name);
		bean.setDataDelegate(dataDelegate);

		return bean;
	}

	private GmMetaModel cortexGmModel() {
		// TODO wrap in a configured model and add non-modifiable MD to everything
		return GMF.getTypeReflection().getModel(_ExplorerCortexModel_.name).getMetaModel();
	}

	// #################################################
	// ## . . . . . . . . Cortex Data . . . . . . . . ##
	// #################################################

	private List<?> cortexData() {
		List<GenericEntity> result = new ArrayList<>();

		AccessDomains accessDomains = access.accessDomains();
		for (String accessId : accessDomains.domainIds())
			result.add(toRxIncrementalAccess(accessDomains.byId(accessId)));

		return result;
	}

	private static RxIncrementalAccess toRxIncrementalAccess(AccessDomain accessDomain) {
		Access access = accessDomain.access();

		RxIncrementalAccess result = RxIncrementalAccess.T.create();
		result.setGlobalId("access:" + access.getAccessId());
		result.setAccessId(access.getAccessId());
		result.setAccessType(access.entityType().getShortName());
		result.setDataModelName(access.getDataModelName());
		result.setServiceModelName(access.getServiceModelName());

		return result;
	}

}
