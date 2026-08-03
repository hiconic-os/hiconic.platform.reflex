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

import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.CollectionTools2.newSet;
import static java.util.Collections.emptySet;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.pr.criteria.matching.StandardMatcher;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.reflection.CloningContext;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.StandardCloningContext;
import com.braintribe.model.meta.data.MetaData;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.extended.EntityMdDescriptor;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.Lazy;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.web.ddra.endpoints.api.WebApiMappingBuilder;
import hiconic.rx.web.ddra.endpoints.api.WebApiMappingRegistry;
import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMapping;
import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMappingImpl;
import hiconic.rx.web.ddra.endpoints.api.v1.WebApiMappingOracle;
import hiconic.rx.webapi.endpoints.OutputPrettiness;
import hiconic.rx.webapi.endpoints.TypeExplicitness;
import hiconic.rx.webapi.model.meta.BooleanOverride;
import hiconic.rx.webapi.model.meta.HideSerializedRequest;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import hiconic.rx.webapi.model.meta.RequestDecodingLenience;
import hiconic.rx.webapi.model.meta.RequestEvaluateWithSession;
import hiconic.rx.webapi.model.meta.RequestMethod;
import hiconic.rx.webapi.model.meta.RequestMapping;
import hiconic.rx.webapi.model.meta.RequestPath;
import hiconic.rx.webapi.model.meta.RequestPathPrefix;
import hiconic.rx.webapi.model.meta.RequestSection;
import hiconic.rx.webapi.model.meta.ResponseAsMultipart;
import hiconic.rx.webapi.model.meta.ResponseAsResourcePayload;
import hiconic.rx.webapi.model.meta.ResponseDepth;
import hiconic.rx.webapi.model.meta.ResponseEntityRecurrenceDepth;
import hiconic.rx.webapi.model.meta.ResponseIncludesEmptyProperties;
import hiconic.rx.webapi.model.meta.ResponseMimeType;
import hiconic.rx.webapi.model.meta.ResponsePreservesTransportPayload;
import hiconic.rx.webapi.model.meta.ResponseProjection;
import hiconic.rx.webapi.model.meta.ResponseTypeExplicitness;
import hiconic.rx.webapi.model.meta.ResponseWithDownloadDialog;

/**
 * Standard {@link WebApiMappingOracle} implementation, backed by {@link ServiceDomains} and meta data like {@link RequestPath} configured on the
 * {@link ServiceRequest}s.
 */
public class StandardWebApiMappingOracle implements WebApiMappingOracle, WebApiMappingRegistry {

	private final Lazy<Map<PathAndMethod, SingleDdraMapping>> mappings = new Lazy<>(() -> new MappingIndexer().buildMappings());
	private final Map<PathAndMethod, SingleDdraMapping> explicitMappings = new ConcurrentHashMap<>();

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
		PathAndMethod key = getKey(pathInfo, method);
		SingleDdraMapping explicitMapping = explicitMappings.get(key);
		return explicitMapping != null ? explicitMapping : mappings.get().get(key);
	}

	private PathAndMethod getKey(String pathInfo, HttpRequestMethod method) {
		return new PathAndMethod(pathInfo, method);
	}

	@Override
	public List<String> getMethods(String pathInfo) {
		return java.util.stream.Stream.concat(explicitMappings.keySet().stream(), mappings.get().keySet().stream()) //
				.filter(k -> k.path().equals(pathInfo)) //
				.map(k -> k.method().name()) //
				.distinct() //
				.sorted() //
				.collect(Collectors.toList());
	}

	@Override
	public List<SingleDdraMapping> getAllForDomain(String serviceDomain) {
		return java.util.stream.Stream.concat(explicitMappings.values().stream(), mappings.get().values().stream()) //
				.filter(m -> java.util.Objects.equals(m.getServiceDomain(), serviceDomain)) //
				.toList();
	}

	@Override
	public WebApiMappingBuilder mapping(String path, HttpRequestMethod method, EntityType<? extends ServiceRequest> requestType) {
		if (StringTools.isEmpty(path) || method == null || requestType == null)
			throw new IllegalArgumentException("Path, method and requestType are required for an explicit Web API mapping.");
		return new ExplicitMappingBuilder(normalizePath(path), method, requestType);
	}

	private String normalizePath(String path) {
		String result = path.startsWith("/") ? path : "/" + path;
		return result.length() > 1 && result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
	}

	private class ExplicitMappingBuilder implements WebApiMappingBuilder {
		private final SingleDdraMappingImpl mapping = new SingleDdraMappingImpl();
		private boolean registered;

		private ExplicitMappingBuilder(String path, HttpRequestMethod method, EntityType<? extends ServiceRequest> requestType) {
			mapping.pathInfo = path;
			mapping.method = method;
			mapping.requestType = requestType;
		}

		@Override public WebApiMappingBuilder serviceDomain(String value) { return configure(() -> mapping.serviceDomain = value); }
		@Override public WebApiMappingBuilder responseProjection(String value) { return configure(() -> mapping.defaultProjection = value); }
		@Override public WebApiMappingBuilder responseMimeType(String value) { return configure(() -> mapping.defaultMimeType = value); }
		@Override public WebApiMappingBuilder downloadResource(boolean value) { return configure(() -> mapping.defaultDownloadResource = value); }
		@Override public WebApiMappingBuilder saveLocally(boolean value) { return configure(() -> mapping.defaultSaveLocally = value); }
		@Override public WebApiMappingBuilder responseFilename(String value) { return configure(() -> mapping.defaultResponseFilename = value); }
		@Override public WebApiMappingBuilder responseContentType(String value) { return configure(() -> mapping.defaultResponseContentType = value); }
		@Override public WebApiMappingBuilder depth(String value) { return configure(() -> mapping.defaultDepth = value); }
		@Override public WebApiMappingBuilder entityRecurrenceDepth(int value) { return configure(() -> mapping.defaultEntityRecurrenceDepth = value); }
		@Override public WebApiMappingBuilder prettiness(OutputPrettiness value) { return configure(() -> mapping.defaultPrettiness = value); }
		@Override public WebApiMappingBuilder typeExplicitness(TypeExplicitness value) { return configure(() -> mapping.defaultTypeExplicitness = value); }
		@Override public WebApiMappingBuilder writeEmptyProperties(boolean value) { return configure(() -> mapping.defaultWriteEmptyProperties = value); }
		@Override public WebApiMappingBuilder writeAbsenceInformation(boolean value) { return configure(() -> mapping.defaultWriteAbsenceInformation = value); }
		@Override public WebApiMappingBuilder stabilizeOrder(boolean value) { return configure(() -> mapping.defaultStabilizeOrder = value); }
		@Override public WebApiMappingBuilder useSessionEvaluation(boolean value) { return configure(() -> mapping.defaultUseSessionEvaluation = value); }
		@Override public WebApiMappingBuilder preserveTransportPayload(boolean value) { return configure(() -> mapping.defaultPreserveTransportPayload = value); }
		@Override public WebApiMappingBuilder decodingLenience(boolean value) { return configure(() -> mapping.defaultDecodingLenience = value); }
		@Override public WebApiMappingBuilder tags(Set<String> value) { return configure(() -> mapping.tags = value == null ? emptySet() : Set.copyOf(value)); }

		private WebApiMappingBuilder configure(Runnable configuration) {
			if (registered)
				throw new IllegalStateException("A registered Web API mapping cannot be changed: " + mapping.pathInfo);
			configuration.run();
			return this;
		}

		@Override
		public void register() {
			if (registered)
				throw new IllegalStateException("Web API mapping builder was already registered: " + mapping.pathInfo);

			PathAndMethod key = getKey(mapping.pathInfo, mapping.method);
			SingleDdraMapping previous = explicitMappings.putIfAbsent(key, mapping);
			if (previous != null)
				throw new IllegalStateException("An explicit Web API mapping is already registered for " + mapping.method + " " + mapping.pathInfo);
			registered = true;
		}
	}

	// ###############################################
	// ## . . . . . . Index all mappings . . . . . .##
	// ###############################################

	private class MappingIndexer {

		private final Map<PathAndMethod, SingleDdraMapping> result = newMap();
		private final Map<PathAndMethod, String> mappingOrigins = newMap();

		private ServiceDomain serviceDomain;
		private CmdResolver cmdResolver;
		private EntityMdResolver requestMdResolver;
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
				requestMdResolver = cmdResolver.getMetaData().entityType(requestType);
				for (MappingMds resolvedMapping : resolveMappingMds()) {
					mappingMds = resolvedMapping;
					if (!mappingMds.hasMappings())
						continue;

					pathInfo = pathInfo();
					for (HttpRequestMethod method : mappingMds.methods()) {
						PathAndMethod key = getKey(pathInfo, method);
						SingleDdraMappingImpl singleMapping = createMappingFromMd(method);
						SingleDdraMapping previous = result.putIfAbsent(key, singleMapping);
						if (previous != null)
							throw duplicateMappingException(key, mappingOrigins.get(key), mappingMds.origin());
						mappingOrigins.put(key, mappingMds.origin());
					}
				}
			}
		}

		private IllegalStateException duplicateMappingException(PathAndMethod key, String previousOrigin, String duplicateOrigin) {
			return new IllegalStateException("Ambiguous Web API mapping for " + key.method() + " " + key.path() + " in service domain '"
					+ serviceDomain.domainId() + "': both " + previousOrigin + " and " + duplicateOrigin + " declare this binding.");
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

			result.hideSerializedRequest = mappingMds.hideSerializedRequest(requestMdResolver.is(HideSerializedRequest.T));
			result.announceAsMultipart = mappingMds.announceAsMultipart(mdProp(ResponseAsMultipart.T, ResponseAsMultipart::getAnnounceAsMultipart));
			result.defaultUseSessionEvaluation = mappingMds.useSessionEvaluation(requestMdResolver.is(RequestEvaluateWithSession.T));
			result.defaultDownloadResource = mappingMds.responseAsResourcePayload(requestMdResolver.is(ResponseAsResourcePayload.T));
			result.defaultDepth = mappingMds.depth(mdProp(ResponseDepth.T, ResponseDepth::getDepth));
			result.defaultEntityRecurrenceDepth = mappingMds.entityRecurrenceDepth(mdProp(ResponseEntityRecurrenceDepth.T, ResponseEntityRecurrenceDepth::getDepth));
			result.defaultWriteEmptyProperties = requestMdResolver.is(ResponseIncludesEmptyProperties.T);
			result.defaultMimeType = mappingMds.responseMimeType(mdProp(ResponseMimeType.T, ResponseMimeType::getMimeType));
			result.defaultPreserveTransportPayload = requestMdResolver.is(ResponsePreservesTransportPayload.T);
			result.defaultDecodingLenience = mappingMds.decodingLenience(requestMdResolver.is(RequestDecodingLenience.T));
			result.defaultProjection = mappingMds.responseProjection(mdProp(ResponseProjection.T, ResponseProjection::getPath));
			result.defaultTypeExplicitness = mdProp(ResponseTypeExplicitness.T, ResponseTypeExplicitness::getTypeExplicitness);
			result.defaultSaveLocally = mappingMds.responseWithDownloadDialog(requestMdResolver.is(ResponseWithDownloadDialog.T));
			result.tags = resolveTags();

			result.transformRequest = mappingMds.transformRequest;

			return result;
		}

		private <R, T extends MetaData> R mdProp(EntityType<T> mdType, Function<T, R> extractor) {
			T md = requestMdResolver.meta(mdType).exclusive();
			return md == null ? null : extractor.apply(md);
		}

		private Set<EntityType<?>> allRequestTypes() {
			var serviceRequestOracle = modelOracle.findEntityTypeOracle(ServiceRequest.T);
			if (serviceRequestOracle == null)
				return Set.of();

			return serviceRequestOracle //
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

		private List<MappingMds> resolveMappingMds() {
			List<RequestPathPrefix> commonPathPrefixes = resolvePathPrefixes();
			List<RequestMapping> completeMappings = requestMdResolver.meta(RequestMapping.T).list();

			List<MappingMds> result = new java.util.ArrayList<>(completeMappings.size() + 1);
			for (int i = 0; i < completeMappings.size(); i++)
				result.add(new MappingMds(completeMappings.get(i), commonPathPrefixes, mappingOrigin("complete RequestMapping #" + (i + 1))));

			MappingMds fineGrainedMapping = new MappingMds();
			fineGrainedMapping.origin = mappingOrigin("fine-grained mapping");
			fineGrainedMapping.pathPrefixes = commonPathPrefixes;
			fineGrainedMapping.path = requestMdResolver.meta(RequestPath.T).exclusive();
			fineGrainedMapping.methods = requestMdResolver.meta(RequestMethod.T).list();

			ServiceRequest transformRequest = getTransformRequest();
			fineGrainedMapping.transformRequest = transformRequest != null ? transformRequest.clone(cloningContext) : null;

			boolean hasExplicitFineGrainedBinding = fineGrainedMapping.path != null || !isEmpty(fineGrainedMapping.methods);

			// Preserve the historic convention where a prefix by itself exposes the request under its (possibly
			// qualified) type name. In the presence of complete mappings, however, the prefix is only a common
			// modifier and must not accidentally create an additional binding.
			if (completeMappings.isEmpty() || hasExplicitFineGrainedBinding)
				result.add(fineGrainedMapping);

			return result;
		}

		/**
		 * Prefix metadata composes across the request hierarchy. This is important for models which express an outer API
		 * namespace and an orthogonal section as separate supertypes, e.g. {@code v1/excerpt}. CMD returns inherited
		 * metadata subtype-first; sorting by inheritance depth puts enclosing prefixes before more specific ones while
		 * preserving declaration order for prefixes at the same depth.
		 */
		private List<RequestPathPrefix> resolvePathPrefixes() {
			Map<String, Integer> inheritanceDepths = inheritanceDepths();

			return requestMdResolver.meta(RequestPathPrefix.T).listExtended().stream() //
					.sorted(Comparator.comparingInt((EntityMdDescriptor md) -> pathPrefixDepth(md, inheritanceDepths)).reversed()) //
					.map(md -> (RequestPathPrefix) md.getResolvedValue()) //
					.toList();
		}

		private int pathPrefixDepth(EntityMdDescriptor md, Map<String, Integer> inheritanceDepths) {
			if (md.getOwnerTypeInfo() == null)
				return Integer.MAX_VALUE; // A configured default is the outermost prefix.

			String ownerTypeSignature = md.getOwnerTypeInfo().addressedType().getTypeSignature();
			return inheritanceDepths.getOrDefault(ownerTypeSignature, 0);
		}

		private Map<String, Integer> inheritanceDepths() {
			Map<String, Integer> result = new HashMap<>();
			Deque<TypeAtDepth> pending = new ArrayDeque<>();
			pending.add(new TypeAtDepth(requestType, 0));

			while (!pending.isEmpty()) {
				TypeAtDepth current = pending.removeFirst();
				String signature = current.type().getTypeSignature();
				Integer previousDepth = result.putIfAbsent(signature, current.depth());
				if (previousDepth != null && previousDepth <= current.depth())
					continue;
				if (previousDepth != null)
					result.put(signature, current.depth());

				for (EntityType<?> superType : current.type().getSuperTypes())
					pending.addLast(new TypeAtDepth(superType, current.depth() + 1));
			}

			return result;
		}

		private String mappingOrigin(String kind) {
			return kind + " on " + requestType.getTypeSignature();
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

		private Set<String> resolveTags() {
			RequestSection section = requestMdResolver.meta(RequestSection.T).exclusive();
			String sectionName = section != null ? section.getName() : null;
			if (mappingMds.completeMapping != null && !StringTools.isEmpty(mappingMds.completeMapping.getSection()))
				sectionName = mappingMds.completeMapping.getSection();
			return StringTools.isEmpty(sectionName) ? emptySet() : asSet(sectionName);
		}

	}

	static class MappingMds {
		public RequestMapping completeMapping;
		public List<RequestPathPrefix> pathPrefixes = List.of();
		public RequestPath path;
		public List<RequestMethod> methods;
		public String origin;

		// TODO support later? (DdraMapping.transformRequest)
		public ServiceRequest transformRequest;

		public MappingMds() {
		}

		public MappingMds(RequestMapping completeMapping, List<RequestPathPrefix> pathPrefixes, String origin) {
			this.completeMapping = completeMapping;
			this.pathPrefixes = pathPrefixes;
			this.origin = origin;
		}

		public String origin() {
			return origin;
		}

		/** Returns an empty string or a prefix that ends with '/' */
		public String pathPrefix() {
			String result = pathPrefixes.stream() //
					.map(RequestPathPrefix::getPrefix) //
					.map(this::withoutSurroundingSlashes) //
					.filter(java.util.Objects::nonNull) //
					.collect(Collectors.joining("/"));
			return result.isEmpty() ? "" : result + "/";
		}

		/** Returns path that doesn't start with '/' or null */
		public String pathWithNoSlashesOrNull() {
			if (completeMapping != null)
				return withoutSurroundingSlashes(completeMapping.getPath());
			if (path == null)
				return null;
			return withoutSurroundingSlashes(path.getPath());
		}

		private String withoutSurroundingSlashes(String value) {
			String result = value;
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
			if (completeMapping != null)
				return List.of(completeMapping.getMethod());
			if (!isEmpty(methods))
				return methods.stream().map(RequestMethod::getMethod).collect(Collectors.toList());
			else
				return Arrays.asList(HttpRequestMethod.GET, HttpRequestMethod.POST);
		}

		public boolean hasMappings() {
			return completeMapping != null || //
					!pathPrefixes.isEmpty() || //
					path != null || //
					!methods.isEmpty() //
			;
		}

		boolean hideSerializedRequest(boolean fallback) { return override(completeMapping == null ? null : completeMapping.getHideSerializedRequest(), fallback); }
		Boolean announceAsMultipart(Boolean fallback) { return override(completeMapping == null ? null : completeMapping.getAnnounceAsMultipart(), fallback); }
		boolean useSessionEvaluation(boolean fallback) { return override(completeMapping == null ? null : completeMapping.getUseSessionEvaluation(), fallback); }
		boolean responseAsResourcePayload(boolean fallback) {
			return override(completeMapping == null ? null : completeMapping.getResponseAsResourcePayload(), fallback);
		}
		String depth(String fallback) { return completeMapping != null ? emptyToFallback(completeMapping.getDepth(), fallback) : fallback; }
		Integer entityRecurrenceDepth(Integer fallback) {
			return completeMapping != null && completeMapping.getEntityRecurrenceDepth() != null ? completeMapping.getEntityRecurrenceDepth() : fallback;
		}
		String responseMimeType(String fallback) {
			return completeMapping != null ? emptyToFallback(completeMapping.getResponseMimeType(), fallback) : fallback;
		}
		String responseProjection(String fallback) { return completeMapping != null ? emptyToNull(completeMapping.getResponseProjection()) : fallback; }
		boolean responseWithDownloadDialog(boolean fallback) {
			return override(completeMapping == null ? null : completeMapping.getResponseWithDownloadDialog(), fallback);
		}
		boolean decodingLenience(boolean fallback) {
			return override(completeMapping == null ? null : completeMapping.getDecodingLenience(), fallback);
		}
		private boolean override(BooleanOverride override, boolean fallback) {
			if (override == null)
				return fallback;
			return switch (override) {
				case INHERIT -> fallback;
				case ENABLED -> true;
				case DISABLED -> false;
			};
		}
		private Boolean override(BooleanOverride override, Boolean fallback) {
			if (override == null)
				return fallback;
			return switch (override) {
				case INHERIT -> fallback;
				case ENABLED -> true;
				case DISABLED -> false;
			};
		}
		private String emptyToNull(String value) { return StringTools.isEmpty(value) ? null : value; }
		private String emptyToFallback(String value, String fallback) { return StringTools.isEmpty(value) ? fallback : value; }
	}

	private static record PathAndMethod(String path, HttpRequestMethod method) {
	}

	private static record TypeAtDepth(EntityType<?> type, int depth) {
	}

}
