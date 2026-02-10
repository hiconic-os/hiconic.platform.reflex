package hiconic.rx.platform.resource;

import static com.braintribe.utils.lcd.NullSafe.nonNull;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Date;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.logging.Logger;
import com.braintribe.model.cache.CacheControl;
import com.braintribe.model.cache.CacheType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.stream.condition.FingerprintMismatch;
import com.braintribe.model.resourceapi.stream.condition.ModifiedSince;
import com.braintribe.model.resourceapi.stream.condition.StreamCondition;

import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.DownloadResourcePayload;
import hiconic.rx.resource.model.api.ExistingResourcePayloadRequest;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;

/**
 * @param <P>
 *            Object denoting the actual payload, using which all relevant data and metadata can be retrieved. For File System storage this is
 *            {@link Path}.
 * 
 * @author peter.gazdik
 */
public abstract class AbstractResourceStorage<P> implements ResourceStorage {

	protected String storageId;

	protected CacheType cacheType = CacheType.privateCache;
	protected Integer cacheMaxAge = Numbers.SECONDS_PER_DAY; // In seconds. Default is 24h.
	protected boolean cacheMustRevalidate = true;

	protected final Logger log = Logger.getLogger(getClass());

	// @formatter:off
	@Required public void setStorageId(String storageId) { this.storageId = storageId; }

	@Configurable public void setCacheType(CacheType cacheType) { this.cacheType =  nonNull(cacheType, "cacheType"); }
	@Configurable public void setCacheMaxAge(Integer cacheMaxAge) { this.cacheMaxAge = cacheMaxAge; }
	@Configurable public void setCacheMustRevalidate(boolean cacheMustRevalidate) { this.cacheMustRevalidate = cacheMustRevalidate; }
	// @formatter:on

	@Override
	public String storageId() {
		return storageId;
	}

	protected abstract Maybe<P> resolvePayload(ExistingResourcePayloadRequest request);

	// ###############################################
	// ## . . . . . . . Get Payload . . . . . . . . ##
	// ###############################################

	@Override
	public Maybe<GetResourcePayloadResponse> getResourcePayload(GetResourcePayload request) {
		Maybe<P> payloadMaybe = resolvePayload(request);
		if (payloadMaybe.isUnsatisfied())
			return payloadMaybe.propagateReason();

		P payload = payloadMaybe.get();

		CacheControl cacheControl = cacheControlFor(payload, request);

		GetResourcePayloadResponse response = GetResourcePayloadResponse.T.create();
		response.setCacheControl(cacheControl);

		if (matchesCondition(cacheControl, request.getCondition()))
			return tryGetPayload(payload, request, response);
		else
			return Maybe.complete(response);
	}

	private Maybe<GetResourcePayloadResponse> tryGetPayload(P payload, GetResourcePayload request, GetResourcePayloadResponse response) {
		try {
			return getPayload(payload, request, response);
		} catch (UncheckedIOException e) {
			return error(IoError.T, e.getMessage());
		}
	}

	protected abstract Maybe<GetResourcePayloadResponse> getPayload(P payload, GetResourcePayload request, GetResourcePayloadResponse response)
			throws UncheckedIOException;

	protected CacheControl cacheControlFor(P payload, DownloadResourcePayload request) {
		CacheControl cacheControl = CacheControl.T.create();

		cacheControl.setType(cacheType);
		cacheControl.setMustRevalidate(cacheMustRevalidate);
		cacheControl.setMaxAge(cacheMaxAge);

		String md5 = getMd5(payload);
		if (md5 == null)
			md5 = request.getMd5();

		Date lastModified = getLastModifiedDate(payload);
		if (lastModified == null)
			lastModified = request.getCreated();

		cacheControl.setFingerprint(md5);
		cacheControl.setLastModified(lastModified);

		return cacheControl;
	}

	/**
	 * In Cortex, these values were taken from the Resource that was retrieved from the access, except for the lastModified date of a FileSystem
	 * resource, which was read from the file. We thus for now keep it that way, but because these resource storage requests only get
	 * {@link ResourceSource} as arguments, we have added extra props on {@link DownloadResourcePayload}.
	 * 
	 * @see DownloadResourcePayload
	 */
	protected String getMd5(@SuppressWarnings("unused") P payload) {
		return null;
	}

	/** @see #getMd5 */

	protected Date getLastModifiedDate(@SuppressWarnings("unused") P payload) {
		return null;
	}

	// ###############################################
	// ## . . . . . . . Store Payload . . . . . . . ##
	// ###############################################

	@Override
	public Maybe<StoreResourcePayloadResponse> storeResourcePayload(StoreResourcePayload request) {
		try {
			return storePayload(request);
		} catch (UncheckedIOException e) {
			return error(IoError.T, e.getMessage());
		}
	}

	protected abstract Maybe<StoreResourcePayloadResponse> storePayload(StoreResourcePayload request) throws UncheckedIOException;

	// ###############################################
	// ## . . . . . . . Delete Payload . . . . . . .##
	// ###############################################

	@Override
	public Maybe<DeleteResourcePayloadResponse> deleteResourcePayload(DeleteResourcePayload request) {
		try {
			return deletePayload(request);
		} catch (UncheckedIOException e) {
			return error(IoError.T, e.getMessage());
		}
	}

	protected abstract Maybe<DeleteResourcePayloadResponse> deletePayload(DeleteResourcePayload request) throws UncheckedIOException;

	// ###############################################
	// #. . . . . . . Match Condition . . . . . . . ##
	// ###############################################

	private boolean matchesCondition(CacheControl cacheControl, StreamCondition streamCondition) {
		if (streamCondition == null)
			return true;

		if (streamCondition instanceof FingerprintMismatch)
			return matchesFingerprintCondition(cacheControl, (FingerprintMismatch) streamCondition);

		if (streamCondition instanceof ModifiedSince)
			return matchesModifiedSinceCondition(cacheControl, (ModifiedSince) streamCondition);

		return true;
	}

	private boolean matchesFingerprintCondition(CacheControl cacheControl, FingerprintMismatch streamCondition) {
		String conditionFingerprint = streamCondition.getFingerprint();
		if (conditionFingerprint == null) {
			log.warn(() -> "A " + FingerprintMismatch.class.getName() + " condition with no fingerprint was provided and will be ignored");
			return true;
		}

		String currentFingerprint = cacheControl.getFingerprint();
		if (currentFingerprint == null) {
			log.warn(() -> "The resource lacks a fingerprint");
			return true;
		}

		boolean matches = currentFingerprint.equals(conditionFingerprint);
		log.trace(() -> "Compared given condition fingerprint [" + conditionFingerprint + "] with resource's fingerprint [" + currentFingerprint
				+ "] . Condition for streaming matched: " + matches);

		return matches;
	}

	private boolean matchesModifiedSinceCondition(CacheControl cacheControl, ModifiedSince streamCondition) {
		Date modifiedSince = streamCondition.getDate();
		if (modifiedSince == null) {
			log.warn(() -> "A " + ModifiedSince.class.getName() + " condition with no date was provided and will be ignored");
			return true;
		}

		Date lastModified = cacheControl.getLastModified();
		if (lastModified == null) {
			log.debug(() -> "No last modified date/time available");
			return true;
		}

		boolean matches = lastModified.after(modifiedSince);
		log.trace(() -> "Compared given modified since condition  [" + modifiedSince + "] with path's last modified time [" + lastModified
				+ "] . Condition for streaming matched: " + matches);

		return matches;
	}

	// ###############################################
	// #. . . . . . . . . Helpers . . . . . . . . . ##
	// ###############################################

	protected <T> Maybe<T> error(EntityType<? extends Reason> errorType, String errorText) {
		return Reasons.build(errorType).text(errorText).toMaybe();
	}

}
