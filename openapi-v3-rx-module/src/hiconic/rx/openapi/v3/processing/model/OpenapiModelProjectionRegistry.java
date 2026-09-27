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
package hiconic.rx.openapi.v3.processing.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverBuilder;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.cmd.context.aspects.RoleAspect;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.Lazy;

import hiconic.rx.module.api.service.ConfiguredModel;

/**
 * Lazily composes the model of a reflected service domain with OpenAPI-only metadata. The projection is immutable and cached per source model, so
 * OpenAPI policy does not have to be written to the shared service model.
 */
public class OpenapiModelProjectionRegistry {

	private final Map<String, ConfiguredModel> projections = new ConcurrentHashMap<>();
	private ConfiguredModel defaultEnrichment;
	private Supplier<AttributeContext> systemAttributeContextSupplier;

	@Required
	public void setDefaultEnrichment(ConfiguredModel defaultEnrichment) {
		this.defaultEnrichment = defaultEnrichment;
	}

	@Required
	public void setSystemAttributeContextSupplier(Supplier<AttributeContext> systemAttributeContextSupplier) {
		this.systemAttributeContextSupplier = systemAttributeContextSupplier;
	}

	public ConfiguredModel acquire(ConfiguredModel source) {
		return projections.computeIfAbsent(source.name(), ignored -> new OpenapiModelProjection(source, defaultEnrichment,
				systemAttributeContextSupplier));
	}

	private static class OpenapiModelProjection implements ConfiguredModel {
		private final Supplier<AttributeContext> systemAttributeContextSupplier;
		private final Lazy<ModelOracle> modelOracle = new Lazy<>(this::buildModelOracle);
		private final Map<Set<String>, CmdResolver> resolversByRoles = new ConcurrentHashMap<>();
		private final GmMetaModel model;

		private OpenapiModelProjection(ConfiguredModel source, ConfiguredModel enrichment,
				Supplier<AttributeContext> systemAttributeContextSupplier) {
			this.systemAttributeContextSupplier = systemAttributeContextSupplier;

			model = ConfigurationModels.create(source.name() + "-openapi-projection") //
					.addDependency(source.modelOracle().getGmMetaModel()) //
					.addDependency(enrichment.modelOracle().getGmMetaModel()) //
					.get();
			model.setVersion(source.modelOracle().getGmMetaModel().getVersion());
		}

		@Override
		public String name() {
			return model.getName();
		}

		@Override
		public CmdResolver systemCmdResolver() {
			return cmdResolver(systemAttributeContextSupplier.get());
		}

		@Override
		public CmdResolver contextCmdResolver() {
			return cmdResolver(AttributeContexts.peek());
		}

		@Override
		public CmdResolver cmdResolver(AttributeContext attributeContext) {
			Set<String> roles = effectiveRoles(attributeContext);
			return resolversByRoles.computeIfAbsent(roles, this::buildCmdResolver);
		}

		private Set<String> effectiveRoles(AttributeContext attributeContext) {
			if (attributeContext == null)
				return Collections.emptySet();

			UserSession userSession = attributeContext.findOrNull(UserSessionAspect.class);
			return userSession == null ? Collections.emptySet() : Set.copyOf(userSession.getEffectiveRoles());
		}

		private CmdResolver buildCmdResolver(Set<String> roles) {
			CmdResolverBuilder builder = CmdResolverImpl.create(modelOracle());
			builder.addDynamicAspectProvider(RoleAspect.class, () -> roles);
			builder.setSessionProvider(() -> roles);
			return builder.done();
		}

		@Override
		public ModelOracle modelOracle() {
			return modelOracle.get();
		}

		private ModelOracle buildModelOracle() {
			return new BasicModelOracle(model);
		}
	}
}
