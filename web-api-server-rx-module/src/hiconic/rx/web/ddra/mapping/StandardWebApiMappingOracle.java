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
package hiconic.rx.web.ddra.mapping;

import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.CollectionTools2.newSet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.pr.criteria.matching.StandardMatcher;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.reflection.CloningContext;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.StandardCloningContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.web.ddra.endpoints.api.api.v1.SingleDdraMapping;
import hiconic.rx.web.ddra.endpoints.api.api.v1.WebApiMappingOracle;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import hiconic.rx.webapi.model.meta.RequestMethod;
import hiconic.rx.webapi.model.meta.RequestPath;
import hiconic.rx.webapi.model.meta.RequestPathPrefix;

/**
 * Standard {@link WebApiMappingOracle} implementation, backed by {@link ServiceDomains} and meta data like {@link RequestPath} configured on the
 * {@link ServiceRequest}s.
 */
public class StandardWebApiMappingOracle implements WebApiMappingOracle {

	private final LazyInitialized<Map<PathAndMethod, SingleDdraMapping>> mappings = new LazyInitialized<>(() -> new MappingIndexer().buildMappings());

	private final CloningContext cloningContext;

	private ServiceDomains serviceDomains;

	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	public StandardWebApiMappingOracle() {
		this.cloningContext = createDefaultCloningContext();
	}

	private static CloningContext createDefaultCloningContext() {
		TraversingCriterion tc = TC.create().negation().joker().done();

		StandardMatcher matcher = new StandardMatcher();
		matcher.setCriterion(tc);

		StandardCloningContext cloningContext = new StandardCloningContext();
		cloningContext.setMatcher(matcher);

		return cloningContext;
	}

	@Override
	public SingleDdraMapping get(String pathInfo, HttpRequestMethod method) {
		return mappings.get().get(getKey(pathInfo, method));
	}

	private PathAndMethod getKey(String pathInfo, HttpRequestMethod method) {
		return new PathAndMethod(pathInfo, method);
	}

	@Override
	public List<String> getMethods(String pathInfo) {
		return mappings.get().keySet().stream() //
				.filter(k -> k.path().equals(pathInfo)) //
				.map(k -> k.method().name()) //
				.sorted() //
				.collect(Collectors.toList());
	}

	@Override
	public List<SingleDdraMapping> getAllForDomain(String serviceDomain) {
		return mappings.get().values().stream() //
				.filter(m -> m.getServiceDomain().equals(serviceDomain)) //
				.toList();
	}

	// ###############################################
	// ## . . . . . . Index all mappings . . . . . .##
	// ###############################################

	private class MappingIndexer {

		private final Map<PathAndMethod, SingleDdraMapping> result = newMap();

		private ServiceDomain serviceDomain;
		private CmdResolver cmdResolver;
		private ModelOracle modelOracle;

		private EntityType<?> requestType;
		private Set<EntityType<?>> requestTypes;
		private Set<EntityType<?>> ambiguousRequestTypes; // types who's short name is not unique within the domain
		private MappingMds mappingMds;

		private String serviceDomainPrefix;
		private String pathInfo;

		private Map<PathAndMethod, SingleDdraMapping> buildMappings() {
			for (ServiceDomain serviceDomain : serviceDomains.list())
				indexDomain(serviceDomain);

			return result;
		}

		private void indexDomain(ServiceDomain _serviceDomain) {
			serviceDomain = _serviceDomain;
			serviceDomainPrefix = "/" + escape(serviceDomain.domainId()) + "/";

			cmdResolver = serviceDomain.systemCmdResolver();
			modelOracle = cmdResolver.getModelOracle();

			requestTypes = allRequestTypes();
			ambiguousRequestTypes = ambiguousRequestTypes();

			for (EntityType<?> _requestType : requestTypes) {
				requestType = _requestType;
				mappingMds = resolveMappingMds();

				if (!mappingMds.hasMappings())
					continue;

				pathInfo = pathInfo();

				for (HttpRequestMethod method : mappingMds.methods()) {
					PathAndMethod key = getKey(pathInfo, method);
					SingleDdraMappingImpl singleMapping = createMappingFromMd(method);

					result.put(key, singleMapping);
				}
			}
		}

		// Should we even allow such characters like ':' or '/' in a service domain?
		private String escape(String s) {
			return s.replace(":", "-") //
					.replace("/", "-") //
			;
		}

		private SingleDdraMappingImpl createMappingFromMd(HttpRequestMethod method) {
			SingleDdraMappingImpl result = new SingleDdraMappingImpl();
			result.serviceDomain = serviceDomain.domainId();
			result.pathInfo = pathInfo;
			result.method = method;
			result.requestType = (EntityType<? extends ServiceRequest>) requestType;

			result.transformRequest = mappingMds.transformRequest;

			return result;
		}

		private Set<EntityType<?>> allRequestTypes() {
			return modelOracle.findEntityTypeOracle(ServiceRequest.T) //
					.getSubTypes() //
					.transitive() //
					.onlyInstantiable() //
					.<EntityType<?>> asTypes();
		}

		private Set<EntityType<?>> ambiguousRequestTypes() {
			Set<EntityType<?>> result = newSet();
			Set<String> visited = newSet();

			for (EntityType<?> requestType : requestTypes)
				if (!visited.add(requestType.getShortName()))
					result.add(requestType);

			return result;
		}

		private MappingMds resolveMappingMds() {
			MappingMds apiMappings = new MappingMds();

			EntityMdResolver requestMdResolver = cmdResolver.getMetaData().entityType(requestType);

			apiMappings.pathPrefix = requestMdResolver.meta(RequestPathPrefix.T).exclusive();
			apiMappings.path = requestMdResolver.meta(RequestPath.T).exclusive();
			apiMappings.methods = requestMdResolver.meta(RequestMethod.T).list();

			ServiceRequest transformRequest = getTransformRequest();
			apiMappings.transformRequest = transformRequest != null ? transformRequest.clone(cloningContext) : null;

			return apiMappings;
		}

		private ServiceRequest getTransformRequest() {
			// TODO support TransformRequest maybe ?
			return null;
		}

		private String pathInfo() {
			String pathPrefix = mappingMds.pathPrefix();

			String path = mappingMds.pathWithNoSlashesOrNull();

			if (path == null)
				if (ambiguousRequestTypes.contains(requestType))
					path = requestType.getTypeSignature();
				else
					path = requestType.getShortName();

			return serviceDomainPrefix + pathPrefix + path;
		}

	}

	static class MappingMds {
		public RequestPathPrefix pathPrefix;
		public RequestPath path;
		public List<RequestMethod> methods;

		// TODO support later?
		public ServiceRequest transformRequest;

		/** Returns an empty string or a prefix that ends with '/' */
		public String pathPrefix() {
			if (pathPrefix == null)
				return "";

			String prefix = pathPrefix.getPrefix();
			if (StringTools.isEmpty(prefix))
				return "";

			if (prefix.startsWith("/"))
				prefix = prefix.substring(1);

			return prefix.endsWith("/") ? prefix : prefix + "/";
		}

		/** Returns path that doesn't start with '/' or null */
		public String pathWithNoSlashesOrNull() {
			if (path == null)
				return null;

			String result = path.getPath();
			if (StringTools.isEmpty(result))
				return null;

			if (result.startsWith("/"))
				result = result.substring(1);

			if (result.endsWith("/"))
				result = result.substring(0, result.length() - 1);

			if (StringTools.isEmpty(result))
				return null;

			return result;
		}

		public List<HttpRequestMethod> methods() {
			if (!isEmpty(methods))
				return methods.stream().map(RequestMethod::getMethod).collect(Collectors.toList());
			else
				return Arrays.asList(HttpRequestMethod.GET, HttpRequestMethod.POST);
		}

		public boolean hasMappings() {
			return pathPrefix != null || //
					path != null || //
					!methods.isEmpty() //
			;
		}
	}

	private static record PathAndMethod(String path, HttpRequestMethod method) {
	}

}
