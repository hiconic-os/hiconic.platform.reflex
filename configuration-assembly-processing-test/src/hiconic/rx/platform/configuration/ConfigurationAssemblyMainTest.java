// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2026
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.configuration;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigurationAssemblyMainTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void assemblesAnEmptyApplicationUsingOnlyFilesystemInputs() throws Exception {
		Path application = temporaryFolder.newFolder("application").toPath();
		Files.createDirectories(application.resolve("classpath-resources"));
		Files.createDirectories(application.resolve("conf"));

		int status = ConfigurationAssemblyMain.run(new String[] { "--application-dir", application.toString() });

		assertThat(status).isZero();
		assertThat(application.resolve("packaged-conf/effective")).isDirectory();
		assertThat(application.resolve("packaged-conf/assembly-report.yaml")).isRegularFile();
	}

	@Test
	public void rejectsIncompleteAndUnknownArguments() {
		assertThat(ConfigurationAssemblyMain.run(new String[0])).isEqualTo(2);
		assertThat(ConfigurationAssemblyMain.run(new String[] {
				"--application-dir", temporaryFolder.getRoot().getAbsolutePath(),
				"--unknown", "value"
		})).isEqualTo(2);
	}
}
