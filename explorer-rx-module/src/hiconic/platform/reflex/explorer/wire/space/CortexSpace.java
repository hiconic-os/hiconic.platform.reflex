package hiconic.platform.reflex.explorer.wire.space;

import java.util.ArrayList;
import java.util.List;

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.platform.reflex._ExplorerCortexModel_;
import hiconic.platform.reflex.explorer.processing.access.StaticNonIncrementalAccess;
import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.explorer.model.configuration.access.ReadOnlySmoodAccess;
import hiconic.rx.explorer.model.cortex.access.RxIncrementalAccess;

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
	private hiconic.platform.reflex.explorer.processing.access.ReadOnlySmoodAccess cortexAccess() {
		StaticNonIncrementalAccess dataDelegate = new StaticNonIncrementalAccess();
		dataDelegate.setModelSupplier(() -> cortexGmModel());
		dataDelegate.setDataSupplier(() -> cortexData());

		hiconic.platform.reflex.explorer.processing.access.ReadOnlySmoodAccess bean = new hiconic.platform.reflex.explorer.processing.access.ReadOnlySmoodAccess();
		bean.setAccessId("cortex");
		bean.setModelName(_ExplorerCortexModel_.name);
		bean.setDataDelegate(dataDelegate);

		return bean;
	}

	private GmMetaModel cortexGmModel() {
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
		result.setAccessId(access.getAccessId());
		result.setAccessType(access.entityType().getShortName());
		result.setDataModelName(access.getDataModelName());
		result.setServiceModelName(access.getServiceModelName());

		return result;
	}

}
