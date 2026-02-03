package hiconic.rx.platform.resource;

import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.resource.source.ResourceSource;

import hiconic.rx.model.service.processing.md.StoreWith;
import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.ExistingResourcePayloadRequest;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.PipeResourcePayload;
import hiconic.rx.resource.model.api.PipeResourcePayloadResponse;
import hiconic.rx.resource.model.api.ResourcePayloadRequest;
import hiconic.rx.resource.model.api.ResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;

/**
 * @author peter.gazdik
 */
public class ResourcePayloadProcessor extends AbstractDispatchingServiceProcessor<ResourcePayloadRequest, ResourcePayloadResponse> {

	private static final Logger log = Logger.getLogger(ResourcePayloadProcessor.class);

	private ServiceDomains serviceDomains;
	private RxResourcesStorages resourceStorages;

	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	@Required
	public void setResourceStorages(RxResourcesStorages resourceStorages) {
		this.resourceStorages = resourceStorages;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<ResourcePayloadRequest, ResourcePayloadResponse> dispatching) {
		dispatching.registerReasoned(StoreResourcePayload.T, this::storeResourcePayload);
		dispatching.registerReasoned(DeleteResourcePayload.T, this::deleteResourcePayload);

		dispatching.registerReasoned(GetResourcePayload.T, this::getResourcePayload);
		dispatching.registerReasoned(PipeResourcePayload.T, this::pipeResourcePayload);
	}

	//
	// Request Delegation
	//

	private Maybe<StoreResourcePayloadResponse> storeResourcePayload(ServiceRequestContext context, StoreResourcePayload request) {
		return resolveStorageForNew(context, request) //
				.flatMap(storage -> storage.storeResourcePayload(request));
	}

	private Maybe<GetResourcePayloadResponse> getResourcePayload(ServiceRequestContext context, GetResourcePayload request) {
		return resolveStorageForExisting(context, request) //
				.flatMap(storage -> storage.getResourcePayload(request));
	}

	private Maybe<PipeResourcePayloadResponse> pipeResourcePayload(ServiceRequestContext context, PipeResourcePayload request) {
		return resolveStorageForExisting(context, request) //
				.flatMap(storage -> storage.pipeResourcePayload(request));
	}

	private Maybe<DeleteResourcePayloadResponse> deleteResourcePayload(ServiceRequestContext context, DeleteResourcePayload request) {
		return resolveStorageForExisting(context, request) //
				.flatMap(storage -> storage.deleteResourcePayload(request));
	}

	//
	// Helpers
	//

	private Maybe<ResourceStorage> resolveStorageForExisting(ServiceRequestContext context, ExistingResourcePayloadRequest request) {
		ResourceSource source = request.getResourceSource();
		EntityType<? extends ResourceSource> sourceType = source.entityType();

		return resolveStorageForSourceType(context, sourceType, source.getUseCase());
	}

	private Maybe<ResourceStorage> resolveStorageForNew(ServiceRequestContext context, StoreResourcePayload request) {
		String storageName = request.getStorageName();
		if (storageName != null) {
			ResourceStorage resourceStorage = resourceStorages.byName(storageName);

			return wrapValueOrNotFound(resourceStorage, () -> "No ResourceStorage found with name: " + storageName);
		}

		final EntityType<? extends ResourceSource> sourceType;
		String sourceTypeSignature = request.getSourceType();
		if (sourceTypeSignature == null) {
			sourceType = ResourceSource.T;
		} else {
			sourceType = GMF.getTypeReflection().findEntityType(sourceTypeSignature);
			if (sourceType == null)
				return notFoundMaybe("No ResourceSource type found with signature: " + sourceTypeSignature);
		}

		return resolveStorageForSourceType(context, sourceType, request.getUseCase());
	}
	private Maybe<ResourceStorage> resolveStorageForSourceType( //
			ServiceRequestContext context, EntityType<? extends ResourceSource> sourceType, String useCase) {

		String domainId = context.getDomainId();
		if (domainId == null)
			return defaultStorageFor(sourceType);

		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		if (serviceDomain == null)
			return notFoundMaybe("No service domain found with id: " + domainId);

		StoreWith storeWith = serviceDomains.byId(domainId) //
				.contextCmdResolver() //
				.getMetaData() //
				.entityType(sourceType) //
				.useCase(useCase) //
				.meta(StoreWith.T) //
				.exclusive();

		if (storeWith == null)
			return defaultStorageFor(sourceType);

		ResourceStorage retrieval = storeWith.getAssociate();

		if (retrieval == null)
			Reasons.build(ConfigurationError.T) //
					.text("No " + ResourceStorage.class.getSimpleName() + " associated with metadata: " + storeWith) //
					.toMaybe();

		log.trace(() -> "Resolved binary retrieval processor " + retrieval + " for source type " + sourceType.getTypeSignature());

		return Maybe.complete(retrieval);
	}

	private Maybe<ResourceStorage> defaultStorageFor(EntityType<? extends ResourceSource> sourceType) {
		return resourceStorages.resolveDefaultStorage(sourceType);
	}

	private static <T> Maybe<T> wrapValueOrNotFound(T value, Supplier<String> textSupplier) {
		if (value != null)
			return Maybe.complete(value);
		else
			return notFoundMaybe(textSupplier.get());
	}

	private static <T> Maybe<T> notFoundMaybe(String text) {
		return Reasons.build(NotFound.T).text(text).toMaybe();
	}
}
