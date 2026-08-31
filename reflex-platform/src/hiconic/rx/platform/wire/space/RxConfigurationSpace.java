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

import java.util.List;
import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.api.ResourceHandle;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxConfigurationContract;
import hiconic.rx.platform.conf.RxConfigurationConstants;
import hiconic.rx.platform.conf.RxConfigurationValueDescriptorExperts;
import hiconic.rx.platform.conf.RxPropertyResolver;
import hiconic.rx.platform.models.RxCmdResolverManager;
import hiconic.rx.platform.models.RxConfiguredModels;
import hiconic.rx.platform.models.RxModelConfigurations;
import hiconic.rx.platform.processing.resource.RxResourcesBuilding.RxUrlResourcesBuilder;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxConfigurationSpace implements RxConfigurationContract {

	@Import
	private RxAuthSpace auth;

	@Import
	private RxApplicationFilesSpace applicationFiles;

	@Import
	private RxPlatformConfigContract platformConfig;

	@Import
	private RxPackagedResourcesSpace packagedResources;

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

	@Override
	public ResourceHandle indexedClasspathResource(String path) {
		return resolveIndexedClasspathResource(platformConfig.classpathIndex(), path);
	}

	static ResourceHandle resolveIndexedClasspathResource(ClasspathIndex classpathIndex, String path) {
		String normalizedPath = normalizeClasspathPath(path);
		List<ClasspathEntry> entries = classpathIndex.forPrefix(normalizedPath).stream()
				.filter(entry -> entry.path.equals(normalizedPath))
				.toList();

		if (entries.isEmpty())
			throw new IllegalArgumentException("Indexed classpath resource not found: " + normalizedPath);
		if (entries.size() > 1)
			throw new IllegalArgumentException("Indexed classpath resource is ambiguous: " + normalizedPath + " (origins: "
					+ entries.stream().map(entry -> entry.origin).toList() + ")");

		return new RxUrlResourcesBuilder(entries.get(0).url);
	}

	private static String normalizeClasspathPath(String path) {
		if (path == null || path.isBlank())
			throw new IllegalArgumentException("Classpath resource path must not be empty");

		String normalized = path.replace('\\', '/');
		while (normalized.startsWith("/"))
			normalized = normalized.substring(1);
		if (normalized.isEmpty() || normalized.contains("../") || normalized.equals(".."))
			throw new IllegalArgumentException("Invalid classpath resource path: " + path);
		return normalized;
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
		bean.setClasspathConfPath(RxConfigurationConstants.CLASSPATH_CONF_PATH);
		bean.setClasspathIndex(platformConfig.classpathIndex());
		bean.setExternalReasonedPropertyLookup(propertyResolver()::resolvePlaceholderReasoned);
		bean.setValueDescriptorExpressionCodec(RxConfigurationValueDescriptorExperts.expressionCodec());
		bean.setValueDescriptorExpertConfigurer(
				registry -> RxConfigurationValueDescriptorExperts.register(registry, packagedResources.resolver(), propertyResolver()));
		return bean;
	}

	@Override
	@Managed
	public RxPropertyResolver propertyResolver() {
		return platformConfig.propertyResolver();
	}

	private Supplier<AttributeContext> systemAttributeContextSupplier() {
		return auth.systemAttributeContextSupplier();
	}

}
