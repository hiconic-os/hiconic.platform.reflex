package hiconic.rx.platform.resource;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;
import static com.braintribe.testing.junit.assertions.gm.assertj.core.api.GmAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.stream.condition.FingerprintMismatch;
import com.braintribe.model.resourceapi.stream.condition.ModifiedSince;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;

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
 * @author peter.gazdik
 */
public class FileSystemStorageTest extends AbstractRxTest {

	// This is also configured in resourc-storage-configuration.yaml
	private static File baseDir = new File("out/resourc-storage");

	@Before
	public void cleanStorage() {
		if (baseDir.exists())
			FileTools.deleteDirectoryRecursivelyUnchecked(baseDir);
	}

	@Test
	public void happyPath() throws Exception {
		String text = "Hello World!";

		ResourceSource resourceSource;
		File file;

		// Store
		{
			Maybe<StoreResourcePayloadResponse> responseMaybe = storePayload(text);
			assertThat(responseMaybe).isSatisfied();

			resourceSource = responseMaybe.get().getResourceSource();
			file = assertFileExists(resourceSource);
			assertFileContent(file, text);
		}

		// Download
		{
			Maybe<GetResourcePayloadResponse> responseMaybe = downloadPayload(resourceSource);
			assertThat(responseMaybe).isSatisfied();

			Resource resource = responseMaybe.get().getResource();
			assertResourceContant(resource, text);
		}

		// Pipe
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Maybe<PipeResourcePayloadResponse> responseMaybe = pipePayload(resourceSource, baos);
			assertThat(responseMaybe).isSatisfied();

			PipeResourcePayloadResponse response = responseMaybe.get();

			assertThat(response.getStreamed()).isTrue();
			assertThat(new String(baos.toByteArray())).isEqualTo(text);
		}

		// Delete
		{

			Maybe<DeleteResourcePayloadResponse> responseMaybe = deletePayload(resourceSource);
			assertThat(responseMaybe).isSatisfied();

			DeleteResourcePayloadResponse response = responseMaybe.get();

			assertThat(response.getDeleted()).isTrue();
			assertThat(file).doesNotExist();
		}
	}

	@Test
	public void nonExistentResource() throws Exception {
		FileSystemSource resourceSource = FileSystemSource.T.create();
		resourceSource.setPath("non/existent");

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

	@Test
	public void nonMatchingMd5() throws Exception {
		String text = "Some Content";

		ResourceSource resourceSource;

		// Store
		{
			Maybe<StoreResourcePayloadResponse> responseMaybe = storePayload(text);
			assertThat(responseMaybe).isSatisfied();

			resourceSource = responseMaybe.get().getResourceSource();
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
	public void nonMatchingCreateDate() throws Exception {
		String text = "Some Content";

		ResourceSource resourceSource;

		// Store
		{
			Maybe<StoreResourcePayloadResponse> responseMaybe = storePayload(text);
			assertThat(responseMaybe).isSatisfied();

			resourceSource = responseMaybe.get().getResourceSource();
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
		storePayload.setSourceType(FileSystemSource.T.getTypeSignature());

		return storePayload.eval(evaluator).getReasoned();
	}

	private File assertFileExists(ResourceSource resourceSource) {
		assertThat(resourceSource).isInstanceOf(FileSystemSource.T);

		FileSystemSource source = (FileSystemSource) resourceSource;
		String path = source.getPath();

		File file = new File(baseDir, path);
		assertThat(file).exists();

		return file;
	}

	private void assertFileContent(File file, String expectedContent) {
		String actualContent = FileTools.read(file).asString();
		assertThat(actualContent).isEqualTo(expectedContent);
	}

	//
	// Download
	//

	private Maybe<GetResourcePayloadResponse> downloadPayload(ResourceSource resourceSource) {
		GetResourcePayload getPayload = GetResourcePayload.T.create();
		getPayload.setResourceSource(resourceSource);

		return getPayload.eval(evaluator).getReasoned();
	}

	private void assertResourceContant(Resource resource, String expectedContent) {
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
