// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.logs.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.braintribe.model.generic.session.InputStreamProvider;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.collection.api.MultiMap;
import com.braintribe.utils.stream.DeleteOnCloseFileInputStream;

public class ZippingInputStreamProvider implements InputStreamProvider {

	private String name;
	private int logFilesCount;
	private Collection<File> files;

	public ZippingInputStreamProvider(String name, MultiMap<String, File> logFiles, int logFilesCount) {
		this.name = name;
		this.logFilesCount = logFilesCount;

		files = new ArrayList<>();
		for (String logFileKey : logFiles.keySet()) {
			files.addAll(logFiles.getAll(logFileKey));
		}
	}
	public ZippingInputStreamProvider(String name, Collection<File> logFiles, int logFilesCount) {
		this.name = name;
		this.logFilesCount = logFilesCount;
		files = new ArrayList<>(logFiles);
	}

	@Override
	public InputStream openInputStream() throws IOException {

		File tempFile = File.createTempFile(name, ".zip");
		//It is a conscious decision that this file is not registered with deleteOnExit
		//The reason is that deleteOnExit creates a permanent reference to the File object
		//that is not removed until the JVM shuts down. This is a potential memory leak.
		//tempFile.deleteOnExit();
		FileTools.deleteFileWhenOrphaned(tempFile);

		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
			int addedLogFiles = 0;
			for (File logFile : files) {
				if (addedLogFiles < logFilesCount) {
					try (FileInputStream in = new FileInputStream(logFile)) {
						out.putNextEntry(new ZipEntry(logFile.getName()));
						IOTools.pump(in, out, 0xffff);
					} finally {
						out.closeEntry();
						addedLogFiles++;
					}
				} else {
					break;
				}
			}

			out.finish();
		}

		return new DeleteOnCloseFileInputStream(tempFile);
	}

}
