package hiconic.rx.platform.resource;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.utils.FileTools;

import hiconic.rx.test.resource.AbstractResourceStorageRxTest;

/**
 * Tests for FsResourceStorage.
 * 
 * @see AbstractResourceStorageRxTest
 */
public class FsResourceStorageTest extends AbstractResourceStorageRxTest<FileSystemSource> {

	// This is also configured in resourc-storage-configuration.yaml
	private static File baseDir = new File("out/resourc-storage");

	@Before
	public void cleanStorage() {
		if (baseDir.exists())
			FileTools.deleteDirectoryRecursivelyUnchecked(baseDir);
	}

	@Override
	protected EntityType<FileSystemSource> resourceSourceType() {
		return FileSystemSource.T;
	}

	//
	// Tests
	//

	// @formatter:off
	@Override @Test public void happyPath()             { super.happyPath(); }
	@Override @Test public void nonExistentResource()   { super.nonExistentResource(); }
	@Override @Test public void nonMatchingMd5()        { super.nonMatchingMd5(); }
	@Override @Test public void nonMatchingCreateDate() { super.nonMatchingCreateDate(); }
	// @formatter:on

	//
	// Implement abstract methods
	//

	@Override
	protected void assertStoredResourceSourceIsOk(FileSystemSource resourceSource, String expectedPayload) {
		File file = assertFileExists(resourceSource);
		assertFileContent(file, expectedPayload);
	}

	@Override
	protected void assertResourceSourceNotExists(FileSystemSource resourceSource) {
		assertFileNotExists(resourceSource);
	}

	@Override
	protected FileSystemSource createNonExistentResourceSource() {
		FileSystemSource result = FileSystemSource.T.create();
		result.setPath("non/existent");
		return result;
	}

	//
	// Helpers
	//

	private File assertFileExists(FileSystemSource resourceSource) {
		File file = getFileFor(resourceSource);
		assertThat(file).exists();

		return file;
	}

	private File assertFileNotExists(FileSystemSource resourceSource) {
		File file = getFileFor(resourceSource);
		assertThat(file).doesNotExist();

		return file;
	}

	private File getFileFor(FileSystemSource resourceSource) {
		String path = resourceSource.getPath();

		return new File(baseDir, path);
	}

	private void assertFileContent(File file, String expectedContent) {
		String actualContent = FileTools.read(file).asString();
		assertThat(actualContent).isEqualTo(expectedContent);
	}

}
