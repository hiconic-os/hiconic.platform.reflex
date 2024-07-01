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
package hiconic.rx.cli.processing;

import static com.braintribe.utils.lcd.StringTools.camelCaseToSocialDistancingCase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.data.mapping.Alias;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;

public class DomainRequestIndexer {
	private Function<String, CmdResolver> domainResolverProvider;
	
	private Map<String, Maybe<Map<String, EntityType<?>>>> indices = new ConcurrentHashMap<>();
	
	public void setDomainResolverProvider(Function<String, CmdResolver> domainResolverProvider) {
		this.domainResolverProvider = domainResolverProvider;
	}
	
	public Maybe<Map<String, EntityType<?>>> getIndex(String domainId) {
		return indices.computeIfAbsent(domainId, this::buildIndex);
	}
	
	private Maybe<Map<String, EntityType<?>>> buildIndex(String domainId) {
	
		Map<String, EntityType<?>> bean = new LinkedHashMap<>();
	
		CmdResolver cmdResolver = domainResolverProvider.apply(domainId);
		
		if (cmdResolver == null)
			Reasons.build(NotFound.T).text("Unknown domain " + domainId);
		
		ModelOracle oracle = cmdResolver.getModelOracle();
		EntityTypeOracle entityTypeOracle = oracle.findEntityTypeOracle(GenericEntity.T);
		GmMetaModel serviceApiModel = entityTypeOracle.asGmEntityType().declaringModel();
	
		Set<GmEntityType> requestTypes = entityTypeOracle.getSubTypes() //
				.transitive() //
				.includeSelf() //
				.onlyInstantiable() //
				.asGmTypes();
	
		for (GmEntityType requestType : requestTypes) {
			if (requestType.getDeclaringModel() != serviceApiModel) {
				EntityType<?> reflectionType = requestType.reflectionType();
	
				String shortcut = camelCaseToSocialDistancingCase(reflectionType.getShortName());
				bean.put(shortcut, reflectionType);
	
				for (Alias alias : cmdResolver.getMetaData().entityType(requestType).meta(Alias.T).list()) {
					String aliasName = alias.getName();
	
					if (aliasName != null)
						bean.put(aliasName, reflectionType);
				}
			}
		}
		
		return Maybe.complete(bean);
	}
}
