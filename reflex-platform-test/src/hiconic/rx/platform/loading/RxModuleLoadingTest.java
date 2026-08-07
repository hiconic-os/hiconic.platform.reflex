// ============================================================================
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
package hiconic.rx.platform.loading;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.loading.samples.api.ApiContract;
import hiconic.rx.platform.loading.samples.api.ApiSpace;
import hiconic.rx.platform.loading.samples.exporter.ExporterSpace;
import hiconic.rx.platform.loading.samples.exporter.ExportingModule;

/**
 * @author peter.gazdik
 */
public class RxModuleLoadingTest {

	@Test
	public void canLoadModule() throws Exception {
		WireContext<?> wireContext = Wire.contextBuilder(ExportingModule.INSTANCE) //
				.bindContract(ApiContract.class, ApiSpace.class) //
				.build();

		RxModuleContract exporterSpace = (RxModuleContract) wireContext.contract();
		assertThat(exporterSpace).isNotNull();
		assertThat(exporterSpace.getClass().getName()).isEqualTo(ExporterSpace.class.getName());
	}

	@Test
	public void resolvesOwningArtifactReflectionFromExplodedTestArtifact() {
		ArtifactReflection reflection = ModuleArtifactReflectionResolver.resolve(ExportingModule.INSTANCE);

		assertThat(reflection.groupId()).isEqualTo("hiconic.platform.reflex");
		assertThat(reflection.artifactId()).isEqualTo("reflex-platform-test");
	}

	@Test
	public void resolvesLegacyModuleCoordinatesFromPackagedSolutions() throws Exception {
		Path application = Files.createTempDirectory("rx-module-reflection-test");
		Path lib = Files.createDirectory(application.resolve("lib"));
		Path jar = Files.createFile(lib.resolve("some-rx-module-1.2.3-rc.jar"));
		Files.writeString(application.resolve("packaged-solutions.txt"), "example.group:some-rx-module#1.2.3-rc\n");

		ArtifactReflection reflection = ModuleArtifactReflectionResolver.reflectionFromLegacyJarName(jar);

		assertThat(reflection.groupId()).isEqualTo("example.group");
		assertThat(reflection.artifactId()).isEqualTo("some-rx-module");
		assertThat(reflection.version()).isEqualTo("1.2.3-rc");
	}

}
