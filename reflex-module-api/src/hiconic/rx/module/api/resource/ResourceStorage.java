package hiconic.rx.module.api.resource;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.resource.Resource;

import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.PipeResourcePayload;
import hiconic.rx.resource.model.api.PipeResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;

/**
 * Expert that can manipulate {@link Resource}s, i.e. storing, loading and modifying the actual binary data.
 * 
 * @author peter.gazdik
 */
public interface ResourceStorage {

	Maybe<StoreResourcePayloadResponse> storeResourcePayload(StoreResourcePayload request);

	Maybe<DeleteResourcePayloadResponse> deleteResourcePayload(DeleteResourcePayload request);

	Maybe<GetResourcePayloadResponse> getResourcePayload(GetResourcePayload request);

	default Maybe<PipeResourcePayloadResponse> pipeResourcePayload(PipeResourcePayload request) {
		return ResourceStorageHelper.pipeResourceViaGetRource(request, this);
	}
}
