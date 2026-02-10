package hiconic.rx.platform.resource;

import static com.braintribe.utils.lcd.CollectionTools2.acquireList;
import static com.braintribe.utils.lcd.CollectionTools2.first;
import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.core.expert.api.MutableDenotationMap;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.utils.lcd.Lazy;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.resource.ResourceStorageDeploymentExpert;

/**
 * Platform registry for {@link ResourceStorage}s.
 * 
 * @author peter.gazdik
 */
public class RxResourcesStorages {

	private final Map<String, Lazy<ResourceStorage>> storages = new ConcurrentHashMap<>();
	private final Map<EntityType<? extends ResourceSource>, List<EntityType<? extends hiconic.rx.resource.model.configuration.ResourceStorage>>> sourceTypeToStorageTypes = new ConcurrentHashMap<>();
	private final Map<EntityType<? extends hiconic.rx.resource.model.configuration.ResourceStorage>, List<Lazy<ResourceStorage>>> storagesByType = new ConcurrentHashMap<>();

	private final MutableDenotationMap<hiconic.rx.resource.model.configuration.ResourceStorage, ResourceStorageDeploymentExpert<?>> deploymentExperts = //
			new PolymorphicDenotationMap<>(true);

	public <RS extends hiconic.rx.resource.model.configuration.ResourceStorage> void registerDeploymentExpert( //
			EntityType<RS> storageType, EntityType<? extends ResourceSource> sourceType, ResourceStorageDeploymentExpert<RS> storage) {

		deploymentExperts.put(storageType, storage);

		acquireList(sourceTypeToStorageTypes, sourceType).add(storageType);
	}

	public void deployLazy(hiconic.rx.resource.model.configuration.ResourceStorage storageDenotation) {
		String storageId = storageDenotation.getStorageId();
		NullSafe.nonNull(storageId, "ResourceStorage.accessId");

		Lazy<ResourceStorage> storageLazy = new Lazy<>(() -> resolveAndRegister(storageDenotation));
		if (storages.putIfAbsent(storageId, storageLazy) != null)
			throw new IllegalArgumentException("Duplicate deployment of a ResourceStorage with storageId: " + storageId);

		EntityType<hiconic.rx.resource.model.configuration.ResourceStorage> storageType = storageDenotation.entityType();
		acquireList(storagesByType, storageType).add(storageLazy);
	}

	private ResourceStorage resolveAndRegister(hiconic.rx.resource.model.configuration.ResourceStorage storageDenotation) {
		ResourceStorageDeploymentExpert<hiconic.rx.resource.model.configuration.ResourceStorage> deploymentExpert = deploymentExperts
				.get(storageDenotation);

		ResourceStorage storage = deploymentExpert.deploy(storageDenotation).get();

		return storage;
	}

	/**
	 * Returns the {@link ResourceStorage} for given storageId or <tt>null</tt> if no such storage exists.
	 * <p>
	 * We might need to address a storage by name if multiple storages existed for the same {@link ResourceSource} type, e.g. SQL based ones
	 * connecting do different databases.
	 */
	public ResourceStorage byId(String storageId) {
		Lazy<ResourceStorage> storageLazy = storages.get(storageId);
		return storageLazy == null ? null : storageLazy.get();
	}

	/** Resolves default {@link ResourceStorage} for the given {@link ResourceSource} type. */
	public Maybe<ResourceStorage> resolveDefaultStorage(EntityType<? extends ResourceSource> sourceType) {
		List<EntityType<? extends hiconic.rx.resource.model.configuration.ResourceStorage>> storageTypes = sourceTypeToStorageTypes.get(sourceType);

		if (isEmpty(storageTypes))
			return notFoundDefault(sourceType, "No storage type registered for this source type.");

		if (storageTypes.size() > 1)
			return notFoundDefault(sourceType,
					"Multiple storage types registered for this source type: " + storageTypes.stream().map(et -> et.getTypeSignature()).toList());

		EntityType<? extends hiconic.rx.resource.model.configuration.ResourceStorage> storageType = first(storageTypes);

		List<Lazy<ResourceStorage>> storages = storagesByType.get(storageType);

		if (isEmpty(storages))
			return notFoundDefault(sourceType, "No ResourceStorage registered for storage type: " + storageType.getTypeSignature());

		if (storages.size() > 1)
			return notFoundDefault(sourceType, "Multiple  ResourceStorages registered exist for storage type: " + storageType.getTypeSignature()
					+ ". Storages: " + storages.stream().map(l -> l.get().storageId()).toList());

		return Maybe.complete(storages.get(0).get());
	}

	private static <T> Maybe<T> notFoundDefault(EntityType<?> sourceType, String text) {
		return error(NotFound.T, "Cannot provide default ResourceStorage for source type: " + sourceType.getTypeSignature() + ". " + text);
	}

	private static <T> Maybe<T> error(EntityType<? extends Reason> reasonType, String text) {
		return Reasons.build(reasonType).text(text).toMaybe();
	}

}
