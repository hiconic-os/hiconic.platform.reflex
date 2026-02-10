package hiconic.rx.messaging.module.wire.space;

import static com.braintribe.utils.lcd.CollectionTools2.newSet;

import java.util.Set;
import java.util.function.Consumer;

import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.common.OptionsEnrichingMarshaller;
import com.braintribe.gm.marshaller.resource.aware.ResourceAwareMarshaller;
import com.braintribe.gm.marshaller.threshold.ThresholdPersistenceMarshaller;
import com.braintribe.model.generic.GMF;
import com.braintribe.transport.messaging.api.MessagingContext;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.messaging.api.MessagingBaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class MessagingBaseRxModuleSpace implements RxModuleContract, MessagingBaseContract {

	@Import
	private RxPlatformContract platform;

	@Override
	@Managed
	public MessagingContext context() {
		MessagingContext bean = new MessagingContext();
		bean.setMarshaller(messageMarshaller());
		bean.setApplicationId(platform.applicationId());
		bean.setNodeId(platform.nodeId());
		return bean;
	}

	@Managed
	private Marshaller messageMarshaller() {
		OptionsEnrichingMarshaller bean = new OptionsEnrichingMarshaller();
		bean.setDelegate(thresholdPersistenceMarshaller());
		bean.setDeserializationOptionsEnricher(o -> o.derive().setRequiredTypesReceiver(requiredTypeEnsurer()).build());

		return bean;
	}

	@Managed
	private Consumer<Set<String>> requiredTypeEnsurer() {
		return allTypes -> {
			Set<String> missingTypes = newSet(allTypes);
			missingTypes.removeIf(ts -> GMF.getTypeReflection().findType(ts) != null);

			if (!missingTypes.isEmpty())
				throw new UnsupportedOperationException(
						"Cannot ensure missing types as there is nowhere to retrieve the meta-model from. Unknown types: " + missingTypes);
		};
	}

	@Managed
	private ThresholdPersistenceMarshaller thresholdPersistenceMarshaller() {
		ThresholdPersistenceMarshaller bean = new ThresholdPersistenceMarshaller();
		bean.setDelegate(resourceAwareMarshaller());
		bean.setSubstituteResourceMarshaller(platform.binMarshaller());
		// TODO
		bean.setThreshold(Long.MAX_VALUE);
		bean.setAccessId("<TODO>");
		// bean.setThreshold(messagingRuntimeProperties.TRIBEFIRE_MESSAGING_TRANSIENT_PERSISTENCE_THRESHOLD());
		// bean.setAccessId(TribefireConstants.ACCESS_TRANSIENT_MESSAGING_DATA);
		bean.setEvaluator(platform.systemEvaluator());

		return bean;
	}

	@Managed
	private ResourceAwareMarshaller resourceAwareMarshaller() {
		ResourceAwareMarshaller bean = new ResourceAwareMarshaller();
		bean.setGmDataMimeType("application/gm");
		bean.setMarshaller(platform.binMarshaller());
		return bean;
	}

}