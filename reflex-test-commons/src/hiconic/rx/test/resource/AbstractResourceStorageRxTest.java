package hiconic.rx.test.resource;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;
import static com.braintribe.testing.junit.assertions.gm.assertj.core.api.GmAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Date;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.stream.condition.FingerprintMismatch;
import com.braintribe.model.resourceapi.stream.condition.ModifiedSince;
import com.braintribe.utils.IOTools;

import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.PipeResourcePayload;
import hiconic.rx.resource.model.api.PipeResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;
import hiconic.rx.test.common.AbstractRxTest;

/**
 * Abstract base that tests a {@link ResourceStorage} implementation.
 * <p>
 * For reference see FsResourceStorageTest in reflext-platform-test.
 */
public abstract class AbstractResourceStorageRxTest<RS extends ResourceSource> extends AbstractRxTest {

	protected abstract EntityType<RS> resourceSourceType();

	@Test
	public void happyPath() {
		String payload = "Hello World!";
		RS resourceSource;

		// Store
		{
			Maybe<StoreResourcePayloadResponse> responseMaybe = storePayload(payload);
			assertThat(responseMaybe).isSatisfied();

			resourceSource = assertCortectStoreResourceSource(responseMaybe);
			assertStoredResourceSourceIsOk(resourceSource, payload);
		}

		// Download
		{
			Maybe<GetResourcePayloadResponse> responseMaybe = downloadPayload(resourceSource);
			assertThat(responseMaybe).isSatisfied();

			Resource resource = responseMaybe.get().getResource();
			assertResourceContent(resource, payload);
		}

		// Pipe
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Maybe<PipeResourcePayloadResponse> responseMaybe = pipePayload(resourceSource, baos);
			assertThat(responseMaybe).isSatisfied();

			PipeResourcePayloadResponse response = responseMaybe.get();

			assertThat(response.getStreamed()).isTrue();
			assertThat(new String(baos.toByteArray())).isEqualTo(payload);
		}

		// Delete
		{

			Maybe<DeleteResourcePayloadResponse> responseMaybe = deletePayload(resourceSource);
			assertThat(responseMaybe).isSatisfied();

			DeleteResourcePayloadResponse response = responseMaybe.get();

			assertThat(response.getDeleted()).isTrue();
			assertResourceSourceNotExists(resourceSource);
		}
	}

	protected abstract void assertStoredResourceSourceIsOk(RS resourceSource, String expectedPayload);

	protected abstract void assertResourceSourceNotExists(RS resourceSource);

	@Test
	public void nonExistentResource() {
		RS resourceSource = createNonExistentResourceSource();

		// Download
		{
			Maybe<GetResourcePayloadResponse> responseMaybe = downloadPayload(resourceSource);
			assertThat(responseMaybe).isUnsatisfiedBy(NotFound.T);
		}

		// Pipe
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Maybe<PipeResourcePayloadResponse> responseMaybe = pipePayload(resourceSource, baos);
			assertThat(responseMaybe).isUnsatisfiedBy(NotFound.T);
		}
	}

	protected abstract RS createNonExistentResourceSource();

	@Test
	public void nonMatchingMd5() {
		String text = "Some Content";

		RS resourceSource;

		// Store
		{
			Maybe<StoreResourcePayloadResponse> responseMaybe = storePayload(text);
			assertThat(responseMaybe).isSatisfied();

			resourceSource = assertCortectStoreResourceSource(responseMaybe);
		}

		FingerprintMismatch fingerprintMismatch = FingerprintMismatch.T.create();
		fingerprintMismatch.setFingerprint("BS-MD5-value");

		// Download
		{
			GetResourcePayload getPayload = GetResourcePayload.T.create();
			getPayload.setMd5("Actual-MD5-value");
			getPayload.setCondition(fingerprintMismatch);
			getPayload.setResourceSource(resourceSource);

			Maybe<GetResourcePayloadResponse> responseMaybe = getPayload.eval(evaluator).getReasoned();
			assertThat(responseMaybe).isSatisfied();
			assertThat(responseMaybe.get().getResource()).isNull();

		}

		// Pipe
		{
			CallStreamCapture capture = CallStreamCapture.T.create();
			capture.setOutputStreamProvider(() -> new ByteArrayOutputStream());

			PipeResourcePayload pipePayload = PipeResourcePayload.T.create();
			pipePayload.setMd5("Actual-MD5-value");
			pipePayload.setCondition(fingerprintMismatch);
			pipePayload.setResourceSource(resourceSource);
			pipePayload.setCapture(capture);

			Maybe<PipeResourcePayloadResponse> responseMaybe = pipePayload.eval(evaluator).getReasoned();
			assertThat(responseMaybe).isSatisfied();
			assertThat(responseMaybe.get().getStreamed()).isFalse();
		}
	}

	@Test
	public void nonMatchingCreateDate() {
		String text = "Some Content";

		RS resourceSource;

		// Store
		{
			Maybe<StoreResourcePayloadResponse> responseMaybe = storePayload(text);
			assertThat(responseMaybe).isSatisfied();

			resourceSource = assertCortectStoreResourceSource(responseMaybe);
		}

		Date now = new Date();
		Date longTimeAgo = new Date(0);

		ModifiedSince modifiedSince = ModifiedSince.T.create();
		modifiedSince.setDate(now);

		// Download
		{
			GetResourcePayload getPayload = GetResourcePayload.T.create();
			getPayload.setCreated(longTimeAgo);
			getPayload.setCondition(modifiedSince);
			getPayload.setResourceSource(resourceSource);

			Maybe<GetResourcePayloadResponse> responseMaybe = getPayload.eval(evaluator).getReasoned();
			assertThat(responseMaybe).isSatisfied();
			assertThat(responseMaybe.get().getResource()).isNull();

		}

		// Pipe
		{
			CallStreamCapture capture = CallStreamCapture.T.create();
			capture.setOutputStreamProvider(() -> new ByteArrayOutputStream());

			PipeResourcePayload pipePayload = PipeResourcePayload.T.create();
			pipePayload.setCreated(longTimeAgo);
			pipePayload.setCondition(modifiedSince);
			pipePayload.setResourceSource(resourceSource);
			pipePayload.setCapture(capture);

			Maybe<PipeResourcePayloadResponse> responseMaybe = pipePayload.eval(evaluator).getReasoned();
			assertThat(responseMaybe).isSatisfied();
			assertThat(responseMaybe.get().getStreamed()).isFalse();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//
	// Store
	//

	private Maybe<StoreResourcePayloadResponse> storePayload(String text) {
		Resource resource = Resource.createTransient(() -> new ByteArrayInputStream(text.getBytes()));

		StoreResourcePayload storePayload = StoreResourcePayload.T.create();
		storePayload.setData(resource);
		storePayload.setSourceType(resourceSourceType().getTypeSignature());

		return storePayload.eval(evaluator).getReasoned();
	}

	private RS assertCortectStoreResourceSource(Maybe<StoreResourcePayloadResponse> responseMaybe) {
		ResourceSource responseSource = responseMaybe.get().getResourceSource();
		assertThat(responseSource).isInstanceOf(resourceSourceType());

		return (RS) responseSource;
	}

	//
	// Download
	//

	private Maybe<GetResourcePayloadResponse> downloadPayload(ResourceSource resourceSource) {
		GetResourcePayload getPayload = GetResourcePayload.T.create();
		getPayload.setResourceSource(resourceSource);

		return getPayload.eval(evaluator).getReasoned();
	}

	private void assertResourceContent(Resource resource, String expectedContent) {
		try (InputStream is = resource.openStream(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			IOTools.pump(is, os);

			String actualContent = new String(os.toByteArray());
			assertThat(actualContent).isEqualTo(expectedContent);

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	//
	// Pipe
	//

	private Maybe<PipeResourcePayloadResponse> pipePayload(ResourceSource resourceSource, ByteArrayOutputStream os) {
		CallStreamCapture capture = CallStreamCapture.T.create();
		capture.setOutputStreamProvider(() -> os);

		PipeResourcePayload pipePayload = PipeResourcePayload.T.create();
		pipePayload.setResourceSource(resourceSource);
		pipePayload.setCapture(capture);

		return pipePayload.eval(evaluator).getReasoned();
	}

	//
	// Delete
	//

	private Maybe<DeleteResourcePayloadResponse> deletePayload(ResourceSource resourceSource) {
		DeleteResourcePayload deletePayload = DeleteResourcePayload.T.create();
		deletePayload.setResourceSource(resourceSource);

		return deletePayload.eval(evaluator).getReasoned();
	}

}
