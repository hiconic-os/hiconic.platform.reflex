package hiconic.rx.module.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.session.OutputStreamProvider;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.IOTools;

import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.PipeResourcePayload;
import hiconic.rx.resource.model.api.PipeResourcePayloadResponse;

/**
 * @author peter.gazdik
 */
/* package */ class ResourceStorageHelper {

	private static final Logger log = Logger.getLogger(ResourceStorageHelper.class);

	static Maybe<PipeResourcePayloadResponse> pipeResourceViaGetRource(PipeResourcePayload request, ResourceStorage resourceStorage) {
		OutputStreamProvider outputStreamProvider = request.getCapture().getOutputStreamProvider();
		if (outputStreamProvider == null)
			return invalidArugment("Cannot pipe request payload, output stream provider is null.");

		GetResourcePayload getPayload = GetResourcePayload.T.create();
		getPayload.setResourceSource(request.getResourceSource());
		getPayload.setRange(request.getRange());
		getPayload.setCondition(request.getCondition());
		getPayload.setDomainId(request.getDomainId());
		getPayload.setSessionId(request.getSessionId());

		Maybe<GetResourcePayloadResponse> responseMaybe = resourceStorage.getResourcePayload(getPayload);
		if (responseMaybe.isUnsatisfied())
			return responseMaybe.propagateReason();

		GetResourcePayloadResponse response = responseMaybe.get();

		Resource resource = response.getResource();
		try (InputStream in = resource.openStream(); OutputStream out = outputStreamProvider.openOutputStream()) {
			IOTools.transferBytes(in, out);
		} catch (IOException e) {
			log.error("Error while transferring resource payload for resource: " + resource, e);
			return Reasons.build(IoError.T).text("Failed to transfer resource payload for resource: " + resource.getName()).toMaybe();
		}

		return pipeResponse(response);
	}

	private static Maybe<PipeResourcePayloadResponse> pipeResponse(GetResourcePayloadResponse response) {
		PipeResourcePayloadResponse result = PipeResourcePayloadResponse.T.create();
		result.setStreamed(true);

		if (response != null) {
			result.setCacheControl(response.getCacheControl());
			result.setRanged(response.getRanged());
			result.setRangeStart(response.getRangeStart());
			result.setRangeEnd(response.getRangeEnd());
			result.setSize(response.getSize());
		}

		return Maybe.complete(result);
	}

	private static <T> Maybe<T> invalidArugment(String text) {
		return Reasons.build(InvalidArgument.T).text(text).toMaybe();
	}

}
