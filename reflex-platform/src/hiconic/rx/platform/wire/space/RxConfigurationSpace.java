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
package hiconic.rx.platform.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.Map;
import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxConfigurationContract;
import hiconic.rx.platform.conf.RxPropertyResolver;
import hiconic.rx.platform.loading.RxPropertiesLoader;
import hiconic.rx.platform.models.RxCmdResolverManager;
import hiconic.rx.platform.models.RxConfiguredModels;
import hiconic.rx.platform.models.RxModelConfigurations;

@Managed
public class RxConfigurationSpace implements RxConfigurationContract {

	@Import
	private RxAuthSpace auth;

	@Import
	private RxMarshallingSpace marshalling;

	@Import
	private RxApplicationFilesSpace applicationFiles;

	@Override
	@Managed
	public RxConfiguredModels configuredModels() {
		RxConfiguredModels bean = new RxConfiguredModels();
		bean.setCmdResolverManager(cmdResolverManager());
		bean.setSystemAttributeContextSupplier(systemAttributeContextSupplier());
		return bean;
	}

	@Override
	public <C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType) {
		return modeledConfiguration().configReasoned(configType);
	}

	@Managed
	RxCmdResolverManager cmdResolverManager() {
		RxCmdResolverManager bean = new RxCmdResolverManager();
		return bean;
	}

	@Managed
	public RxModelConfigurations modelConfigurations() {
		RxModelConfigurations bean = new RxModelConfigurations();
		bean.setConfiguredModels(configuredModels());
		return bean;
	}

	@Managed
	private ModeledYamlConfiguration modeledConfiguration() {
		ModeledYamlConfiguration bean = new ModeledYamlConfiguration();
		bean.setConfigFolder(applicationFiles.confPath().toFile());
		bean.setClasspathConfPath("HICONIC-CONF/");
		bean.setClasspathIndex(new ClasspathIndex());
		bean.setExternalReasonedPropertyLookup(propertyResolver()::resolveReasoned);
		return bean;
	}

	@Managed
	RxPropertyResolver propertyResolver() {
		RxPropertyResolver bean = new RxPropertyResolver();

		Map<String, String> rawProperties = getOrTunnel(
				RxPropertiesLoader.loadFromFolder(applicationFiles.confPath().toFile(), "properties(-.*)?.yaml", marshalling.yamlMarshaller()));
		bean.setRawProperties(rawProperties);
		return bean;
	}

	private Supplier<AttributeContext> systemAttributeContextSupplier() {
		return auth.systemAttributeContextSupplier();
	}

}
