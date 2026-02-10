package hiconic.rx.platform.resource;

import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.resource.source.ResourceSource;

import hiconic.rx.model.service.processing.md.StoreWith;
import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
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
 * Processor for {@link ResourcePayloadRequest}s
 * <p>
 * It resolves the right {@link ResourceStorage} from the ones registered in the platform via {@link RxPlatformConfigurator} and delegates the request
 * to it.
 * 
 * @author peter.gazdik
 */
public class ResourcePayloadProcessor extends AbstractDispatchingServiceProcessor<ResourcePayloadRequest, ResourcePayloadResponse> {

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
		String storageId = request.getStorageId();
		if (storageId != null) {
			ResourceStorage resourceStorage = resourceStorages.byId(storageId);

			return wrapValueOrNotFound(resourceStorage, () -> "No ResourceStorage found with storageId: " + storageId);
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
			return defaultStorageFor(sourceType, domainId);

		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		if (serviceDomain == null)
			return notFoundMaybe("No service domain found with id: " + domainId);

		CmdResolver cmdResolver = serviceDomains.byId(domainId).contextCmdResolver();

		StoreWith storeWith = cmdResolver//
				.getMetaData() //
				.lenient(true) //
				.entityType(sourceType) //
				.useCase(useCase) //
				.meta(StoreWith.T) //
				.exclusive();

		if (storeWith == null) {
			if (cmdResolver.getModelOracle().findEntityTypeOracle(sourceType) == null)
				return error(InvalidArgument.T, "Source type [" + sourceType.getTypeSignature() + "] not part of service domain [" + domainId + "]");

			return defaultStorageFor(sourceType, domainId);
		}

		ResourceStorage storage = storeWith.getResourceStorage();
		if (storage == null) {
			String storageId = storeWith.getStorageId();
			if (storageId == null)
				return error(ConfigurationError.T, "No storage nor storageId configured on type [" + sourceType.getTypeSignature() + "], domainId: ["
						+ domainId + "], metadata: " + storeWith);

			storage = resourceStorages.byId(storageId);
			if (storage == null)
				return error(ConfigurationError.T, "Storage not found by configured storageId [" + storageId + "] on type ["
						+ sourceType.getTypeSignature() + "], domainId: [" + domainId + "], metadata: " + storeWith);

			storeWith.setResourceStorage(storage);
		}

		return Maybe.complete(storage);
	}

	private Maybe<ResourceStorage> defaultStorageFor(EntityType<? extends ResourceSource> sourceType, String domainId) {
		Maybe<ResourceStorage> storageMaybe = resourceStorages.resolveDefaultStorage(sourceType);
		if (storageMaybe.isSatisfied())
			return storageMaybe;

		Reason reason = storageMaybe.whyUnsatisfied();
		return error(NotFound.T, "Could not find default storage for domain: " + domainId, reason);
	}

	private static <T> Maybe<T> wrapValueOrNotFound(T value, Supplier<String> textSupplier) {
		if (value != null)
			return Maybe.complete(value);
		else
			return notFoundMaybe(textSupplier.get());
	}

	private static <T> Maybe<T> notFoundMaybe(String text) {
		return error(NotFound.T, text);
	}

	private static <T> Maybe<T> error(EntityType<? extends Reason> reasonType, String text) {
		return Reasons.build(reasonType).text(text).toMaybe();
	}

	private static <T> Maybe<T> error(EntityType<? extends Reason> reasonType, String text, Reason cause) {
		return Reasons.build(reasonType).text(text).cause(cause).toMaybe();
	}
}
